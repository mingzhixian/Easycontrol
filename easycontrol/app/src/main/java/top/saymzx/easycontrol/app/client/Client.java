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
import top.saymzx.easycontrol.app.FullActivity;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class Client {
  private final ExecutorService executor = new ThreadPoolExecutor(3, 15, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new CustomThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

  // 连接
  private Adb adb;
  private AdbStream stream;

  // 子服务
  VideoDecode videoDecode;
  AudioDecode audioDecode;
  Controller controller;

  private final ClientView clientView = new ClientView(this);

  // 是否正常解码播放
  boolean isNormalPlay = true;

  public Client(Device device, UsbDevice usbDevice) {
    Dialog dialog = AppData.publicTools.createClientLoading(AppData.main, () -> handleEvent(Event_CLOSE));
    dialog.show();
    videoOutLoopNum = videoOutLoopNumLimit = device.maxFps;
    new Thread(() -> {
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
        // 启动子服务
        executor.execute(this::executeVideoDecodeOut);
        if (audioDecode != null) executor.execute(this::executeAudioDecodeOut);
        executor.execute(this::executeStreamIn);
        // 更新UI
        AppData.main.runOnUiThread(dialog::cancel);
        createUI();
        // 阻塞等待连接断开
        synchronized (stream) {
          stream.wait();
        }
      } catch (Exception e) {
        AppData.main.runOnUiThread(() -> {
          if (dialog.isShowing()) dialog.cancel();
          Toast.makeText(AppData.main, e.toString(), Toast.LENGTH_SHORT).show();
          Log.e("easycontrol", e.toString());
          handleEvent(Event_CLOSE);
        });
      }
    }).start();
  }

  // 连接ADB
  private void connectADB(Device device, UsbDevice usbDevice) throws Exception {
    executor.execute(() -> {
      try {
        // 连接和授权总共超时时间为10秒
        Thread.sleep(10000);
        if (adb == null) handleEvent(Event_CLOSE);
      } catch (InterruptedException ignored) {
        handleEvent(Event_CLOSE);
      }
    });
    if (usbDevice == null) {
      Pair<String, Integer> address = AppData.publicTools.getIpAndPort(device.address);
      if (address == null) throw new Exception("地址格式错误");
      // 连接ADB
      adb = new Adb(InetAddress.getByName(address.first).getHostAddress(), address.second, AppData.keyPair);
    } else adb = new Adb(usbDevice, AppData.keyPair);
  }

  // 发送Server
  private void sendServer() throws Exception {
    // 尝试发送Server
    try {
      for (int i = 0; i < 3; i++) {
        String isHaveServer = adb.runAdbCmd("ls -l /data/local/tmp/easycontrol_*", true);
//      if (isHaveServer.contains("easycontrol_server_" + AppData.versionCode + ".jar")) return;
        adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/easycontrol_server_" + AppData.versionCode + ".jar");
        return;
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
      adb.runAdbCmd("CLASSPATH=/data/local/tmp/easycontrol_server_" + AppData.versionCode + ".jar app_process / top.saymzx.easycontrol.server.Server" +
        " max_size=" + device.maxSize +
        " max_fps=" + device.maxFps +
        " video_bit_rate=" + device.maxVideoBit +
        " turn_off_screen=" + (AppData.setting.getSlaveTurnOffScreen() ? 1 : 0) +
        " set_width=" + setWidth +
        " set_Height=" + setHeight +
        " > /dev/null 2>&1 & ", false);
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
        stream = adb.localSocketForward("easycontrol", true);
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
      // 视频大小
      clientView.videoSize = new Pair<>(stream.readInt(), stream.readInt());
      // 视频解码
      stream.readByte();
      ByteBuffer csd0 = readFrame();
      stream.readByte();
      ByteBuffer csd1 = readFrame();
      videoDecode = new VideoDecode(clientView.videoSize, csd0, csd1);
      // 音频解码
      if (stream.readByte() == 1) {
        stream.readByte();
        csd0 = readFrame();
        audioDecode = new AudioDecode(csd0);
      }
      // 控制
      controller = new Controller(this::handleEvent, stream);
    } catch (Exception e) {
      throw new Exception("启动Client失败" + e);
    }
  }

  // 创建UI
  private void createUI() {
    handleEvent(AppData.setting.getDefaultFull() ? Event_CHANGE_FULL : Event_CHANGE_SMALL);
  }

  // 服务分发
  private void executeStreamIn() {
    try {
      boolean hasData = true;
      while (hasData) {
        switch (stream.readByte()) {
          case 0:

          case 1:
            ByteBuffer videoFrame = readFrame();
            executor.execute(() -> videoDecode.decodeIn(videoFrame));
            break;
          case 2:
            ByteBuffer audioFrame = readFrame();
            executor.execute(() -> audioDecode.decodeIn(audioFrame));
            break;
          case 3:
            controller.handleClipboardEvent();
            break;
          case 4:
            controller.handleRotationNotification();
            break;
        }
        hasData = stream.isEmpty();
      }
      executor.execute(this::executeStreamIn);
    } catch (IOException | InterruptedException ignored) {
      handleEvent(Event_CLOSE);
    }
  }

  private int videoOutLoopNum;
  private final int videoOutLoopNumLimit;

  private void executeVideoDecodeOut() {
    videoDecode.decodeOut(isNormalPlay);
    videoOutLoopNum++;
    if (videoOutLoopNum > videoOutLoopNumLimit) {
      controller.checkClipBoard();
      videoOutLoopNum = 0;
    }
    if (clientView.touchTime < 30) {
      adb.sendMoreOk(stream);
      clientView.touchTime++;
    }
    executor.execute(this::executeVideoDecodeOut);
  }

  private void executeAudioDecodeOut() {
    audioDecode.decodeOut(isNormalPlay);
    executor.execute(this::executeAudioDecodeOut);
  }

  private ByteBuffer readFrame() throws InterruptedException, IOException {
    return stream.readByteArray(stream.readInt());
  }

  public static final int Event_CHANGE_FULL = 1;
  public static final int Event_CHANGE_SMALL = 2;
  public static final int Event_CHANGE_MINI = 3;
  public static final int Event_CHANGE_ROTATION = 4;
  public static final int Event_UPDATE_SURFACE = 5;
  public static final int Event_UPDATE_SURFACE_SIZE = 6;
  public static final int Event_CLOSE = 7;

  public void handleEvent(int arg) {
    AppData.main.runOnUiThread(() -> {
      switch (arg) {
        case Event_CHANGE_FULL:
          clientView.changeToFull();
          break;
        case Event_CHANGE_SMALL:
          clientView.changeToSmall();
          break;
        case Event_CHANGE_MINI:
          clientView.changeToMini();
          break;
        case Event_CHANGE_ROTATION:
          clientView.changeRotation();
          break;
        case Event_UPDATE_SURFACE:
          clientView.updateSurface();
          break;
        case Event_UPDATE_SURFACE_SIZE:
          clientView.updateSurfaceSize();
          break;
        case Event_CLOSE:
          clientView.hide();
          release();
          break;
      }
    });
  }

  public interface MyFunctionInt {
    void handleEvent(int arg);
  }

  private void release() {
    executor.shutdownNow();
    if (adb != null) adb.close();
    if (videoDecode != null) videoDecode.release();
    if (audioDecode != null) audioDecode.release();
  }

  // 线程创建模板
  static class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setPriority(Thread.MAX_PRIORITY);
      return thread;
    }
  }
}
