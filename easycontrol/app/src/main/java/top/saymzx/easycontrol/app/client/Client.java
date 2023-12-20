package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.net.Socket;
import java.util.ArrayList;

import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
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
  ClientStream clientStream;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private HandlerThread handlerThread;
  private Handler handler;
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public Controller controller = new Controller(this::write);
  private final ClientView clientView;

  public Client(Device device) {
    allClient.add(this);
    // 初始化
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      handlerThread = new HandlerThread("easycontrol_mediacodec");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
    }
    clientView = new ClientView(this, device.setResolution);
    Dialog dialog = PublicTools.createClientLoading(AppData.main);
    dialog.show();
    // 启动线程
    Thread startThread = new Thread(() -> {
      try {
        // 解析地址
        Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
        // 启动server
        startServer(device, address);
        // 连接server
        connectServer(device, address);
        // 更新UI
        AppData.handler.post(dialog::cancel);
        createUI(device);
      } catch (Exception e) {
        if (dialog.isShowing()) AppData.handler.post(dialog::cancel);
        release(String.valueOf(e));
      }
    });
    // 启动Client
    startThread.start();
  }

  // 启动Server
  private void startServer(Device device, Pair<String, Integer> address) throws Exception {
    adb = new Adb(address.first, address.second, AppData.keyPair);
    if (BuildConfig.ENABLE_DEBUG_FEATURE || !adb.runAdbCmd("ls -l /data/local/tmp/easycontrol_*", true).contains(AppData.serverName)) {
      adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), AppData.serverName);
    }
    adb.runAdbCmd("app_process -Djava.class.path=" + AppData.serverName + " / top.saymzx.easycontrol.server.Server"
      + " tcpPort=" + (address.second + 1)
      + " isAudio=" + (device.isAudio ? 1 : 0)
      + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " turnOffScreen=" + (device.turnOffScreen ? 1 : 0)
      + " autoLockAfterControl=" + (device.autoLockAfterControl ? 1 : 0)
      + " useH265=" + ((device.useH265 && supportH265) ? 1 : 0)
      + " useOpus=" + ((device.useOpus && supportOpus) ? 1 : 0) + " > /dev/null 2>&1 &", false);
  }

  // 连接Server
  private void connectServer(Device device, Pair<String, Integer> address) throws Exception {
    Thread.sleep(50);
    for (int i = 0; i < 60; i++) {
      try {
        if (device.useTunnel) {
          AdbStream adbStream = adb.tcpForward(address.second + 1, true);
          clientStream = new ClientStream(adb, adbStream);
        } else {
          Socket socket = new Socket(address.first, address.second + 1);
          clientStream = new ClientStream(socket);
        }
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  // 创建UI
  private void createUI(Device device) {
    AppData.handler.post(() -> {
      if (device.defaultFull) clientView.changeToFull();
      else clientView.changeToSmall();
    });
  }

  // 启动子服务
  public void startSubService() {
    // 运行中
    status = 1;
    // 启动子服务
    executeStreamInThread.start();
    AppData.handler.post(this::executeOtherService);
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
      if (clientStream.readByte() == 1) useOpus = clientStream.readByte() == 1;
      boolean useH265 = clientStream.readByte() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (clientStream.readByte()) {
          case VIDEO_EVENT:
            if (videoDecode != null) videoDecode.decodeIn(clientStream.readFrame(), clientStream.readLong());
            else {
              if (videoCsd == null) {
                videoCsd = new Pair<>(clientStream.readFrame(), clientStream.readLong());
                if (useH265) videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), videoCsd, null, handler);
              } else videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), videoCsd, new Pair<>(clientStream.readFrame(), clientStream.readLong()), handler);
            }
            break;
          case AUDIO_EVENT:
            if (audioDecode != null) audioDecode.decodeIn(clientStream.readFrame());
            else audioDecode = new AudioDecode(useOpus, clientStream.readFrame(), handler);
            break;
          case CLIPBOARD_EVENT:
            controller.nowClipboardText = new String(clientStream.readByteArray(clientStream.readInt()));
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controller.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(clientStream.readInt(), clientStream.readInt());
            AppData.handler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
        }
      }
    } catch (Exception ignored) {
      release(AppData.main.getString(R.string.error_stream_closed));
    }
  }

  private void executeOtherService() {
    if (status == 1) {
      controller.checkClipBoard();
      controller.sendKeepAlive();
      AppData.handler.postDelayed(this::executeOtherService, 1500);
    }
  }

  private void write(byte[] buffer) {
    try {
      clientStream.write(buffer);
    } catch (Exception ignored) {
      release(AppData.main.getString(R.string.error_stream_closed));
    }
  }

  public void release(String error) {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
    if (error != null) {
      Log.e("Easycontrol", error);
      AppData.handler.post(() -> Toast.makeText(AppData.main, error, Toast.LENGTH_SHORT).show());
    }
    for (int i = 0; i < 4; i++) {
      try {
        switch (i) {
          case 0:
            executeStreamInThread.interrupt();
            if (handlerThread != null) handlerThread.quit();
            break;
          case 1:
            AppData.handler.post(() -> clientView.hide(true));
            break;
          case 2:
            clientStream.close();
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

  public static void recover(String addressStr, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Pair<String, Integer> address = PublicTools.getIpAndPort(addressStr);
        Adb adb1 = new Adb(address.first, address.second, AppData.keyPair);
        adb1.runAdbCmd("wm size reset", true);
        adb1.close();
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");
}
