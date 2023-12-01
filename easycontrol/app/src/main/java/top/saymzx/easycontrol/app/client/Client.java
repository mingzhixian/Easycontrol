package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.view.ClientView;
import top.saymzx.easycontrol.app.client.view.FullActivity;
import top.saymzx.easycontrol.app.client.view.SmallView;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class Client {
  // 状态，0为初始，1为连接，-1为关闭
  private int status = 0;

  // 连接
  private Adb adb;
  ClientStream mainClientStream;
  ClientStream videoClientStream;

  // 子服务
  private final ArrayList<Thread> threads = new ArrayList<>();
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public Controller controller;
  private final ClientView clientView;
  private static final int timeoutDelay = 1000 * 10;
  private static final boolean supportH265 = PublicTools.isH265DecoderSupport();

  public Client(Device device) {
    // 初始化
    clientView = new ClientView(this, device.setResolution);
    Dialog dialog = PublicTools.createClientLoading(AppData.main);
    dialog.show();
    // 启动超时监听
    AppData.handler.postDelayed(() -> {
      if (status == 0) {
        if (dialog.isShowing()) dialog.cancel();
        Toast.makeText(AppData.main, "连接超时", Toast.LENGTH_SHORT).show();
        release();
      }
    }, timeoutDelay);
    // 启动Client
    new Thread(() -> {
      try {
        // 解析地址
        Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
        // 启动server
        startServer(device, address);
        // 连接server
        connectServer(device, address);
        // 创建子服务
        createSubService();
        // 更新UI
        AppData.main.runOnUiThread(dialog::cancel);
        createUI(device);
      } catch (Exception e) {
        if (dialog.isShowing()) AppData.main.runOnUiThread(dialog::cancel);
        String error = String.valueOf(e);
        Log.e("Easycontrol", error);
        AppData.main.runOnUiThread(() -> Toast.makeText(AppData.main, error, Toast.LENGTH_SHORT).show());
        release();
      }
    }).start();
  }

  // 启动Server
  private void startServer(Device device, Pair<String, Integer> address) throws Exception {
    adb = new Adb(address.first, address.second, AppData.keyPair);
    adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), AppData.serverName);
    float reSize = device.setResolution ? (device.defaultFull ? FullActivity.getResolution() : SmallView.getResolution()) : -1;
    adb.runAdbCmd("app_process -Djava.class.path=" + AppData.serverName + " / top.saymzx.easycontrol.server.Server"
      + " tcpPort=" + (address.second + 1)
      + " isAudio=" + (device.isAudio ? 1 : 0)
      + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " turnOffScreen=" + (device.turnOffScreen ? 1 : 0)
      + " autoControlScreen=" + (device.autoControlScreen ? 1 : 0)
      + " reSize=" + reSize
      + " useH265=" + ((device.useH265 && supportH265) ? 1 : 0) + " > /dev/null 2>&1 &", false);
  }

  // 连接Server
  private void connectServer(Device device, Pair<String, Integer> address) throws Exception {
    Thread.sleep(100);
    for (int i = 0; i < 60; i++) {
      try {
        if (device.useTunnel) {
          if (mainClientStream == null) {
            AdbStream adbStream = adb.tcpForward(address.second + 1, true);
            mainClientStream = new ClientStream(adb, adbStream);
          }
          if (videoClientStream == null) {
            AdbStream adbStream = adb.tcpForward(address.second + 1, true);
            mainClientStream = new ClientStream(adb, adbStream);
          }
        } else {
          if (mainClientStream == null) {
            Socket socket = new Socket(address.first, address.second + 1);
            mainClientStream = new ClientStream(socket);
          }
          if (videoClientStream == null) {
            Socket socket = new Socket(address.first, address.second + 1);
            videoClientStream = new ClientStream(socket);
          }
        }
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception("连接Server失败");
  }

  // 创建子服务
  private void createSubService() throws IOException, InterruptedException {
    // 控制
    controller = new Controller(this);
    // 是否支持H265编码
    boolean isH265Support = mainClientStream.readByte() == 1;
    // 视频大小
    if (mainClientStream.readByte() != CHANGE_SIZE_EVENT) throw new IOException("启动Client失败:数据错误-应为CHANGE_SIZE_EVENT");
    Pair<Integer, Integer> newVideoSize = new Pair<>(mainClientStream.readInt(), mainClientStream.readInt());
    clientView.updateVideoSize(newVideoSize);
    // 视频解码
    Pair<byte[], Long> csd0 = new Pair<>(videoClientStream.readFrame(), videoClientStream.readLong());
    Pair<byte[], Long> csd1 = null;
    if (!isH265Support) {
      csd1 = new Pair<>(videoClientStream.readFrame(), videoClientStream.readLong());
    }
    videoDecode = new VideoDecode(newVideoSize, csd0, csd1);
    // 音频解码
    if (mainClientStream.readByte() == 1) {
      if (mainClientStream.readByte() != AUDIO_EVENT) throw new IOException("启动Client失败:数据错误-应为AUDIO_EVENT");
      audioDecode = new AudioDecode(mainClientStream.readFrame());
    }
    threads.add(new Thread(this::executeVideoDecodeIn));
    threads.add(new Thread(this::executeVideoDecodeOut));
    if (audioDecode != null) threads.add(new Thread(this::executeAudioDecodeOut));
    threads.add(new Thread(this::executeStreamIn));
  }

  // 创建UI
  private void createUI(Device device) {
    AppData.main.runOnUiThread(() -> {
      if (device.defaultFull) clientView.changeToFull();
      else clientView.changeToSmall();
    });
  }

  // 启动子服务
  public void startSubService(Surface surface) {
    videoDecode.setSurface(surface);
    // 启动子服务
    for (Thread thread : threads) {
      thread.setPriority(Thread.MAX_PRIORITY);
      thread.start();
    }
    AppData.handler.post(this::executeOtherService);
    // 运行中
    status = 1;
  }

  // 服务分发
  private static final int AUDIO_EVENT = 1;
  private static final int CLIPBOARD_EVENT = 2;
  private static final int CHANGE_SIZE_EVENT = 3;

  private void executeStreamIn() {
    try {
      while (!Thread.interrupted()) {
        switch (mainClientStream.readByte()) {
          case AUDIO_EVENT:
            byte[] audioFrame = mainClientStream.readFrame();
            if (clientView.checkIsNeedPlay()) audioDecode.decodeIn(audioFrame);
            break;
          case CLIPBOARD_EVENT:
            controller.nowClipboardText = new String(mainClientStream.readByteArray(mainClientStream.readInt()));
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controller.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(mainClientStream.readInt(), mainClientStream.readInt());
            AppData.main.runOnUiThread(() -> clientView.updateVideoSize(newVideoSize));
            break;
        }
      }
    } catch (Exception ignored) {
      release();
    }
  }

  private void executeVideoDecodeIn() {
    try {
      while (!Thread.interrupted()) videoDecode.decodeIn(videoClientStream.readFrame(), videoClientStream.readLong());
    } catch (Exception ignored) {
      release();
    }
  }

  private void executeVideoDecodeOut() {
    while (!Thread.interrupted()) videoDecode.decodeOut(clientView.checkIsNeedPlay());
  }

  private void executeAudioDecodeOut() {
    while (!Thread.interrupted()) audioDecode.decodeOut();
  }


  private void executeOtherService() {
    controller.checkClipBoard();
    controller.sendKeepAlive();
    AppData.handler.postDelayed(this::executeOtherService, 1500);
  }

  public void write(byte[] buffer) {
    try {
      mainClientStream.write(buffer);
    } catch (Exception ignored) {
      release();
    }
  }

  public void release() {
    if (status == -1) return;
    status = -1;
    for (int i = 0; i < 6; i++) {
      try {
        switch (i) {
          case 0:
            for (Thread thread : threads) thread.interrupt();
            break;
          case 1:
            clientView.hide(true);
            break;
          case 2:
            mainClientStream.close();
            videoClientStream.close();
            break;
          case 3:
            adb.close();
            break;
          case 4:
            videoDecode.release();
            break;
          case 5:
            audioDecode.release();
            break;
        }
      } catch (Exception ignored) {
      }
    }
  }
}
