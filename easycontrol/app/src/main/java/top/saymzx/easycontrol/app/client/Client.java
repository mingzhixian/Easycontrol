package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.util.ArrayList;

import top.saymzx.adb.Adb;
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
  final ClientStream clientStream;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private HandlerThread handlerThread;
  private Handler handler;
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public Controller controller = new Controller(this::write);
  private final ClientView clientView;
  public static Device device;

  public Client(Device device) {
    Client.device = device;
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
    // 连接
    clientStream = new ClientStream(device, result -> AppData.uiHandler.post(() -> {
      dialog.cancel();
      if (result) {
        if (device.defaultFull) clientView.changeToFull();
        else clientView.changeToSmall();
      } else release();
    }));
  }

  // 检查是否启动完成
  public boolean isStarted() {
    return status == 1 && clientView != null;
  }

  // 启动子服务
  public void startSubService() {
    // 运行中
    status = 1;
    // 启动子服务
    executeStreamInThread.start();
    AppData.uiHandler.post(this::executeOtherService);
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
            AppData.uiHandler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
        }
      }
    } catch (Exception ignored) {
      PublicTools.logToast(AppData.main.getString(R.string.error_stream_closed));
      release();
    }
  }

  private void executeOtherService() {
    if (status == 1) {
      controller.checkClipBoard();
      controller.sendKeepAlive();
      AppData.uiHandler.postDelayed(this::executeOtherService, 1500);
    }
  }

  private void write(byte[] buffer) {
    try {
      clientStream.write(buffer);
    } catch (Exception ignored) {
      PublicTools.logToast(AppData.main.getString(R.string.error_stream_closed));
      release();
    }
  }

  public void release() {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
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
            clientStream.close();
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

  // 保存悬浮窗位置及大小
  public static void writeDb(int x, int y, int width, int height) {
    try {
      device.window_x = x;
      device.window_y = y;
      device.window_width = width;
      device.window_height = height;
      AppData.dbHelper.update(device);
    } catch (Exception e) {
      Log.e("Easycontrol", String.valueOf(e));
    }
  }

}
