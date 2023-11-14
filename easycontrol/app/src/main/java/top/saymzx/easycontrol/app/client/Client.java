package top.saymzx.easycontrol.app.client;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.view.ClientView;
import top.saymzx.easycontrol.app.client.view.FullActivity;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class Client {
  // 连接，双连接，避免tcp对头阻塞，以及避免adb同步阻塞
  private Adb adb;
  private Adb videoAdb;
  private AdbStream stream;
  private AdbStream videoStream;

  // 子服务
  public VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public Controller controller;

  private final ClientView clientView = new ClientView(this);

  // 是否正常解码播放
  public boolean isNormalPlay = true;
  private boolean startSuccess = false;

  private final Thread startCheckThread;
  private final ExecutorService executor = new ThreadPoolExecutor(4, 6, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new CustomThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
  private final Dialog dialog = PublicTools.createClientLoading(AppData.main, this::errorClose);

  public Client(Device device, UsbDevice usbDevice) {
    // 显示加载框
    dialog.show();
    // 启动超时
    startCheckThread = new Thread(() -> {
      try {
        Thread.sleep(8000);
        if (!startSuccess) errorClose();
      } catch (InterruptedException ignored) {
      }
    });
    startCheckThread.start();
    // 启动Client
    executor.execute(() -> {
      try {
        // 连接ADB
        connectADB(device, usbDevice);
        // 发送server
        sendServer();
        // 启动server
        startServer(device);
        // 连接server
        connectServer();
        // 创建子服务
        createSubService();
        // 更新UI
        AppData.main.runOnUiThread(dialog::cancel);
        createUI(device);
        startSuccess = true;
      } catch (Exception e) {
        String error = String.valueOf(e);
        Log.e("Easycontrol", error);
        AppData.main.runOnUiThread(() -> Toast.makeText(AppData.main, error, Toast.LENGTH_SHORT).show());
        errorClose();
        throw new RuntimeException(e);
      }
    });
  }

  // 连接ADB
  private void connectADB(Device device, UsbDevice usbDevice) throws Exception {
    if (usbDevice == null) {
      Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
      if (address == null) throw new Exception("地址格式错误");
      // 连接ADB
      adb = new Adb(InetAddress.getByName(address.first).getHostAddress(), address.second, AppData.keyPair);
      videoAdb = new Adb(InetAddress.getByName(address.first).getHostAddress(), address.second, AppData.keyPair);
    } else {
      adb = new Adb(usbDevice, AppData.keyPair);
      videoAdb = adb;
    }
  }

  // 发送Server
  private void sendServer() throws Exception {
    try {
      if (BuildConfig.ENABLE_DEBUG_FEATURE) {
        adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/" + AppData.serverName);
      } else {
        for (int i = 0; i < 3; i++) {
          String isHaveServer = adb.runAdbCmd("ls -l /data/local/tmp/easycontrol_*", true);
          if (isHaveServer.contains(AppData.serverName)) return;
          adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/" + AppData.serverName);
        }
      }
    } catch (Exception ignored) {
      throw new Exception("发送Server失败");
    }
  }

  // 启动Server
  private void startServer(Device device) throws Exception {
    try {
      int setWidth = -1;
      int setHeight = -1;
      if (device.setResolution) {
        Pair<Integer, Integer> tmpDeviceSize = FullActivity.getScreenSizeWithoutDock();
        setWidth = tmpDeviceSize.first;
        setHeight = tmpDeviceSize.second;
      }
      adb.runAdbCmd("CLASSPATH=/data/local/tmp/" + AppData.serverName + " app_process / top.saymzx.easycontrol.server.Server" + " is_audio=" + (device.isAudio ? 1 : 0) + " max_size=" + device.maxSize + " max_fps=" + device.maxFps + " video_bit_rate=" + device.maxVideoBit + " turn_off_screen=" + (device.turnOffScreen ? 1 : 0) + " auto_control_screen=" + (device.autoControlScreen ? 1 : 0) + " set_width=" + setWidth + " set_height=" + setHeight + " isH265DecoderSupport=" + (PublicTools.isH265DecoderSupport() ? 1 : 0) + " > /dev/null 2>&1 & ", false);
    } catch (Exception ignored) {
      throw new Exception("启动Server失败");
    }
  }

  // 连接Server
  private void connectServer() throws Exception {
    Thread.sleep(100);
    // 尝试连接
    for (int i = 0; i < 50; i++) {
      try {
        if (stream == null) stream = adb.localSocketForward("easycontrol", true);
        if (videoStream == null) videoStream = videoAdb.localSocketForward("easycontrol", true);
        return;
      } catch (Exception ignored) {
        Thread.sleep(40);
      }
    }
    throw new Exception("连接Server失败");
  }

  // 创建子服务
  private void createSubService() throws Exception {
    try {
      // 是否支持H265编码
      boolean isH265Support = videoStream.readByte() == 1;
      // 视频大小
      clientView.videoSize = new Pair<>(videoStream.readInt(), videoStream.readInt());
      // 视频解码
      Pair<Long, ByteBuffer> csd0 = readFrame(videoStream);
      Pair<Long, ByteBuffer> csd1 = isH265Support ? null : readFrame(videoStream);
      videoDecode = new VideoDecode(clientView.videoSize, csd0, csd1);
      // 音频解码
      if (stream.readByte() == 1) {
        if (stream.readByte() != 1) throw new IOException("数据错误");
        csd0 = readFrame(stream);
        audioDecode = new AudioDecode(csd0);
      }
      // 控制
      controller = new Controller(clientView, stream);
    } catch (Exception e) {
      throw new Exception("启动Client失败" + e);
    }
  }

  // 创建UI
  private void createUI(Device device) {
    AppData.main.runOnUiThread(() -> {
      if (device.defaultFull) clientView.changeToFull();
      else clientView.changeToSmall();
    });
  }

  // 启动子服务
  public void startSubService() {
    startCheckThread.interrupt();
    executor.execute(this::executeVideoDecodeIn);
    executor.execute(this::executeVideoDecodeOut);
    if (audioDecode != null) {
      executor.execute(this::executeAudioDecodeIn);
      executor.execute(this::executeAudioDecodeOut);
    }
    executor.execute(this::executeStreamIn);
    executor.execute(this::executeOtherService);
  }

  // 服务分发
  private void executeStreamIn() {
    try {
      while (!Thread.interrupted()) {
        switch (stream.readByte()) {
          case 1:
            Pair<Long, ByteBuffer> audioFrame = readFrame(stream);
            if (isNormalPlay) audioDecode.dataQueue.offer(audioFrame);
            break;
          case 2:
            controller.handleClipboardEvent();
            break;
          case 3:
            controller.handleRotationNotification();
            break;
        }
      }
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private void executeVideoDecodeIn() {
    try {
      while (!Thread.interrupted()) {
        videoAdb.sendMoreOk(videoStream);
        videoDecode.decodeIn(readFrame(videoStream));
      }
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private void executeVideoDecodeOut() {
    try {
      while (!Thread.interrupted()) videoDecode.decodeOut(isNormalPlay);
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private void executeAudioDecodeIn() {
    try {
      while (!Thread.interrupted()) audioDecode.decodeIn();
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private void executeAudioDecodeOut() {
    try {
      while (!Thread.interrupted()) audioDecode.decodeOut();
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private void executeOtherService() {
    try {
      while (!Thread.interrupted()) {
        controller.checkClipBoard();
        controller.sendKeepAlive();
        checkScreenRotation();
        Thread.sleep(1000);
      }
    } catch (Exception ignored) {
      errorClose();
    }
  }

  private Boolean lastScreenIsPortal = null;

  private void checkScreenRotation() {
    Pair<Integer, Integer> screenSize = PublicTools.getScreenSize();
    boolean nowScreenIsPortal = screenSize.first < screenSize.second;
    if (lastScreenIsPortal != null && lastScreenIsPortal != nowScreenIsPortal) {
      AppData.main.runOnUiThread(() -> clientView.reCalculateSite(screenSize));
    }
    lastScreenIsPortal = nowScreenIsPortal;
  }

  private void errorClose() {
    AppData.main.runOnUiThread(() -> clientView.hide(true));
  }

  private Pair<Long, ByteBuffer> readFrame(AdbStream stream) throws InterruptedException, IOException {
    long pts = stream.readLong();
    return new Pair<>(pts, stream.readByteArray(stream.readInt()));
  }

  public void release() {
    if (dialog.isShowing()) dialog.cancel();
    executor.shutdownNow();
    if (adb != null) adb.close();
    if (videoAdb != null) videoAdb.close();
    if (videoDecode != null) videoDecode.release();
    if (audioDecode != null) audioDecode.release();
  }

  // 线程创建模板
  private static class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setPriority(Thread.MAX_PRIORITY);
      return thread;
    }
  }
}
