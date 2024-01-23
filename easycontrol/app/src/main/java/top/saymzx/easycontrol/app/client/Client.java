package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.adb.Adb;
import top.saymzx.easycontrol.app.buffer.BufferStream;
import top.saymzx.easycontrol.app.client.view.ClientView;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class Client {
  // 状态，0为初始，1为连接，-1为关闭
  private int status = 0;
  public static final ArrayList<Client> allClient = new ArrayList<>();

  // 连接
  private Adb adb;
  private BufferStream mainBufferStream;
  private BufferStream videoBufferStream;
  private BufferStream shell;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private final Thread executeVideoStreamInThread = new Thread(this::executeVideoStreamIn);
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public final ControlPacket controlPacket = new ControlPacket(this::write);
  public final ClientView clientView;
  public final String uuid;
  private Thread startThread;
  private Thread timeOutThread;

  private static final String serverName = "/data/local/tmp/easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");

  public Client(Device device) {
    this(device, null);
  }

  public Client(Device device, UsbDevice usbDevice) {
    allClient.add(this);
    uuid = device.uuid;
    Dialog dialog = PublicTools.createClientLoading(AppData.main);
    // 超时
    timeOutThread = new Thread(() -> {
      try {
        Thread.sleep(10 * 1000);
        if (startThread != null) startThread.interrupt();
        if (dialog.isShowing()) dialog.cancel();
        release(null);
      } catch (InterruptedException ignored) {
      }
    });
    // 界面
    clientView = new ClientView(device, controlPacket, () -> {
      status = 1;
      executeVideoStreamInThread.setPriority(Thread.MAX_PRIORITY);
      executeVideoStreamInThread.start();
      executeStreamInThread.start();
      AppData.uiHandler.post(this::executeOtherService);
    }, () -> release(null));
    // 连接
    startThread = new Thread(() -> {
      try {
        adb = connectADB(device, usbDevice);
        startServer(device);
        connectServer();
        AppData.uiHandler.post(() -> {
          if (device.defaultFull) clientView.changeToFull();
          else clientView.changeToSmall();
        });
      } catch (Exception e) {
        release(e.toString());
      } finally {
        dialog.cancel();
        if (timeOutThread != null) timeOutThread.interrupt();
      }
    });
    // 启动
    dialog.show();
    timeOutThread.start();
    startThread.start();
  }

  // 连接ADB
  private static Adb connectADB(Device device, UsbDevice usbDevice) throws Exception {
    if (usbDevice == null) {
      Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
      return new Adb(address.first, address.second, AppData.keyPair);
    } else return new Adb(usbDevice, AppData.keyPair);
  }

  // 启动Server
  private void startServer(Device device) throws Exception {
    if (BuildConfig.ENABLE_DEBUG_FEATURE || !adb.runAdbCmd("ls /data/local/tmp/easycontrol_*").contains(serverName)) {
      adb.runAdbCmd("rm /data/local/tmp/easycontrol_* ");
      adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), serverName);
    }
    shell = adb.getShell();
    shell.write(ByteBuffer.wrap(("app_process -Djava.class.path=" + serverName + " / top.saymzx.easycontrol.server.Server"
      + " isAudio=" + (device.isAudio ? 1 : 0) + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " keepAwake=" + (AppData.setting.getKeepAwake() ? 1 : 0)
      + " useH265=" + ((device.useH265 && supportH265) ? 1 : 0)
      + " useOpus=" + ((device.useOpus && supportOpus) ? 1 : 0) + " \n").getBytes()));
  }

  // 连接Server
  private void connectServer() throws Exception {
    Thread.sleep(50);
    for (int i = 0; i < 60; i++) {
      try {
        if (mainBufferStream == null) mainBufferStream = adb.localSocketForward("easycontrol");
        // 为了减少adb同步阻塞的问题，此处分开音视频流
        if (videoBufferStream == null) videoBufferStream = adb.localSocketForward("easycontrol");
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  // 服务分发
  private static final int AUDIO_EVENT = 1;
  private static final int CLIPBOARD_EVENT = 2;
  private static final int CHANGE_SIZE_EVENT = 3;

  private void executeStreamIn() {
    try {
      // 音视频流参数
      boolean useOpus = true;
      if (mainBufferStream.readByte() == 1) useOpus = mainBufferStream.readByte() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (mainBufferStream.readByte()) {
          case AUDIO_EVENT:
            ByteBuffer audioFrame = controlPacket.readFrame(mainBufferStream);
            if (audioDecode != null) audioDecode.decodeIn(audioFrame);
            else audioDecode = new AudioDecode(useOpus, audioFrame);
            break;
          case CLIPBOARD_EVENT:
            controlPacket.nowClipboardText = new String(mainBufferStream.readByteArray(mainBufferStream.readInt()).array());
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controlPacket.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(mainBufferStream.readInt(), mainBufferStream.readInt());
            AppData.uiHandler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
        }
      }
    } catch (Exception ignored) {
      release(AppData.main.getString(R.string.error_stream_closed));
    }
  }

  private void executeVideoStreamIn() {
    try {
      boolean useH265 = videoBufferStream.readByte() == 1;
      ByteBuffer csd0 = controlPacket.readFrame(videoBufferStream);
      if (useH265) videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), csd0, null);
      else videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), csd0, controlPacket.readFrame(videoBufferStream));
      while (!Thread.interrupted()) videoDecode.decodeIn(controlPacket.readFrame(videoBufferStream));
    } catch (Exception ignored) {
    }
  }

  private void executeOtherService() {
    if (status == 1) {
      controlPacket.checkClipBoard();
      controlPacket.sendKeepAlive();
      AppData.uiHandler.postDelayed(this::executeOtherService, 1500);
    }
  }

  private void write(ByteBuffer byteBuffer) {
    try {
      mainBufferStream.write(byteBuffer);
    } catch (Exception ignored) {
      release(AppData.main.getString(R.string.error_stream_closed));
    }
  }

  public void release(String error) {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
    if (error != null) PublicTools.logToast(error);
    try {
      Log.e("Easycontrol", new String(shell.readAllBytes().array()));
    } catch (Exception ignored) {
    }
    AppData.uiHandler.post(() -> clientView.hide(true));
    executeStreamInThread.interrupt();
    executeVideoStreamInThread.interrupt();
    if (mainBufferStream != null) mainBufferStream.close();
    if (videoBufferStream != null) videoBufferStream.close();
    if (adb != null) adb.close();
    if (videoDecode != null) videoDecode.release();
    if (audioDecode != null) audioDecode.release();
  }

  public static void runOnceCmd(Device device, UsbDevice usbDevice, String cmd, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device, usbDevice);
        adb.runAdbCmd(cmd);
        adb.close();
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static void restartOnTcpip(Device device, UsbDevice usbDevice, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device, usbDevice);
        String output = adb.restartOnTcpip(5555);
        adb.close();
        handle.run(output.contains("restarting"));
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

}
