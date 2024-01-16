package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

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
  private BufferStream bufferStream;
  private BufferStream shell;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private HandlerThread handlerThread;
  private Handler handler;
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  private final ControlPacket controlPacket = new ControlPacket(this::write);
  public final ClientView clientView;
  public final String uuid;

  private static final String serverName = "/data/local/tmp/easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");

  public Client(Device device, UsbDevice usbDevice) {
    allClient.add(this);
    // 初始化
    uuid = device.uuid;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      handlerThread = new HandlerThread("easycontrol_mediacodec");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
    }
    clientView = new ClientView(device, controlPacket, () -> {
      status = 1;
      executeStreamInThread.start();
      AppData.uiHandler.post(this::executeOtherService);
    }, () -> release(null));
    Dialog dialog = PublicTools.createClientLoading(AppData.main);
    dialog.show();
    // 连接
    Thread timeOutThread = new Thread(() -> {
      try {
        Thread.sleep(10 * 1000);
        if (dialog.isShowing()) AppData.uiHandler.post(dialog::cancel);
        release(null);
      } catch (InterruptedException ignored) {
      }
    });
    new Thread(() -> {
      try {
        adb = connectADB(device, usbDevice);
        startServer(device);
        connectServer();
        AppData.uiHandler.post(() -> {
          if (device.defaultFull) clientView.changeToFull();
          else clientView.changeToSmall();
        });
        timeOutThread.interrupt();
      } catch (Exception e) {
        release(Arrays.toString(e.getStackTrace()));
      } finally {
        dialog.cancel();
      }
    }).start();
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
      adb.pushFileByShell(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), serverName);
    }
    shell = adb.getShell();
    shell.write(ByteBuffer.wrap(("app_process -Djava.class.path=" + serverName + " / top.saymzx.easycontrol.server.Server"
      + " isAudio=" + (device.isAudio ? 1 : 0)
      + " maxSize=" + device.maxSize
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
        bufferStream = adb.localSocketForward("easycontrol");
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  // 服务分发
  private static final int VIDEO_EVENT = 1;
  private static final int AUDIO_EVENT = 2;
  private static final int CLIPBOARD_EVENT = 3;
  private static final int CHANGE_SIZE_EVENT = 4;

  private void executeStreamIn() {
    try {
      Pair<byte[], Long> videoCsd = null;
      // 音视频流参数
      boolean useOpus = true;
      if (bufferStream.readByte() == 1) useOpus = bufferStream.readByte() == 1;
      boolean useH265 = bufferStream.readByte() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (bufferStream.readByte()) {
          case VIDEO_EVENT:
            byte[] videoFrame = controlPacket.readFrame(bufferStream);
            if (videoDecode != null) videoDecode.decodeIn(videoFrame, bufferStream.readLong());
            else {
              if (useH265) videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), new Pair<>(videoFrame, bufferStream.readLong()), null, handler);
              else {
                if (videoCsd == null) videoCsd = new Pair<>(videoFrame, bufferStream.readLong());
                else videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), videoCsd, new Pair<>(videoFrame, bufferStream.readLong()), handler);
              }
            }
            break;
          case AUDIO_EVENT:
            byte[] audioFrame = controlPacket.readFrame(bufferStream);
            if (audioDecode != null) audioDecode.decodeIn(audioFrame);
            else audioDecode = new AudioDecode(useOpus, audioFrame, handler);
            break;
          case CLIPBOARD_EVENT:
            controlPacket.nowClipboardText = new String(bufferStream.readByteArray(bufferStream.readInt()).array());
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controlPacket.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(bufferStream.readInt(), bufferStream.readInt());
            AppData.uiHandler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
        }
      }
    } catch (Exception ignored) {
      String serverError = "";
      try {
        serverError = new String(shell.readAllBytes().array());
      } catch (IOException | InterruptedException ignored1) {
      }
      release(AppData.main.getString(R.string.error_stream_closed) + serverError);
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
      bufferStream.write(byteBuffer);
    } catch (Exception ignored) {
      String serverError = "";
      try {
        serverError = new String(shell.readAllBytes().array());
      } catch (IOException | InterruptedException ignored1) {
      }
      release(AppData.main.getString(R.string.error_stream_closed) + serverError);
    }
  }

  public void release(String error) {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
    if (error != null) PublicTools.logToast(error);
    for (int i = 0; i < 4; i++) {
      try {
        switch (i) {
          case 0:
            executeStreamInThread.interrupt();
            if (handlerThread != null) handlerThread.quit();
            break;
          case 1:
            AppData.uiHandler.post(() -> clientView.hide(true));
            break;
          case 2:
            bufferStream.close();
            adb.close();
            break;
          case 3:
            videoDecode.release();
            if (audioDecode != null) audioDecode.release();
            break;
        }
      } catch (Exception ignored) {
      }
    }
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
