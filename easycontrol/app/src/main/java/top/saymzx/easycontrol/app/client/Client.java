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

public class Client {
  private final ExecutorService executor = new ThreadPoolExecutor(3, 15, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new CustomThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

  // 连接
  public Adb adb;
  public AdbStream stream;

  // 子服务
  public VideoDecode videoDecode;
  public AudioDecode audioDecode;
  public Controller controller;

  private final ClientView clientView = new ClientView(this);

  // 是否正常解码播放
  public boolean isNormalPlay = true;

  private final Dialog dialog = AppData.publicTools.createClientLoading(AppData.main, () -> clientView.hide(true));

  public Client(Device device, UsbDevice usbDevice) {
//    try {
//      UsbChannel usbChannel=new UsbChannel(usbDevice);
//      usbChannel.write(new byte[]{1});
//      byte[] bytes=usbChannel.read(1);
//      Log.e("sdsd", Arrays.toString(bytes));
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
    // 显示加载框
    dialog.show();
    // 启动Client
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
        // 更新UI
        AppData.main.runOnUiThread(dialog::cancel);
        createUI();
        // 阻塞等待连接断开
        synchronized (stream) {
          stream.wait();
        }
      } catch (Exception e) {
        AppData.main.runOnUiThread(() -> {
          Toast.makeText(AppData.main, e.toString(), Toast.LENGTH_SHORT).show();
          Log.e("easycontrol", e.toString());
          clientView.hide(true);
        });
      }
    }).start();
  }

  // 连接ADB
  private void connectADB(Device device, UsbDevice usbDevice) throws Exception {
    executor.execute(() -> {
      try {
        // 连接和授权总共超时时间为5秒
        Thread.sleep(5000);
      } catch (InterruptedException ignored) {
      }
      if (adb == null) AppData.main.runOnUiThread(() -> clientView.hide(true));
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
      adb.runAdbCmd("CLASSPATH=/data/local/tmp/" + AppData.serverName + " app_process / top.saymzx.easycontrol.server.Server" +
        " is_audio=" + (device.isAudio ? 1 : 0) +
        " max_size=" + device.maxSize +
        " max_fps=" + device.maxFps +
        " video_bit_rate=" + device.maxVideoBit +
        " turn_off_screen=" + (AppData.setting.getSlaveTurnOffScreen() ? 1 : 0) +
        " auto_control_screen=" + (AppData.setting.getAutoControlScreen() ? 1 : 0) +
        " set_width=" + setWidth +
        " set_height=" + setHeight +
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
      controller = new Controller(clientView, stream);
    } catch (Exception e) {
      throw new Exception("启动Client失败" + e);
    }
  }

  // 启动子服务
  public void startSubService() {
    executor.execute(this::executeVideoDecodeOut);
    if (audioDecode != null) executor.execute(this::executeAudioDecodeOut);
    executor.execute(this::executeStreamIn);
    executor.execute(this::executeOtherService);
  }

  // 创建UI
  private void createUI() {
    AppData.main.runOnUiThread(() -> {
      if (AppData.setting.getDefaultFull()) clientView.changeToFull();
      else clientView.changeToSmall();
    });
  }

  // 服务分发
  private void executeStreamIn() {
    try {
      boolean hasData = true;
      while (hasData) {
        switch (stream.readByte()) {
          case 1:
            ByteBuffer videoFrame = readFrame();
            // 视频因在界面旋转时会重新生成配置参数，因此此处仍需解码，只是不再显示，音频则可以直接停止解码
            executor.execute(() -> videoDecode.decodeIn(videoFrame));
            break;
          case 2:
            ByteBuffer audioFrame = readFrame();
            if (isNormalPlay) executor.execute(() -> audioDecode.decodeIn(audioFrame));
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
      AppData.main.runOnUiThread(() -> clientView.hide(true));
    }
  }

  private void executeVideoDecodeOut() {
    videoDecode.decodeOut(isNormalPlay);
    executor.execute(this::executeVideoDecodeOut);
  }

  private void executeAudioDecodeOut() {
    audioDecode.decodeOut();
    executor.execute(this::executeAudioDecodeOut);
  }

  private void executeOtherService() {
    controller.checkClipBoard();
    controller.sendKeepAlive();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      clientView.hide(true);
    }
    executor.execute(this::executeOtherService);
  }

  private ByteBuffer readFrame() throws InterruptedException, IOException {
    return stream.readByteArray(stream.readInt());
  }

  public void release() {
    if (dialog.isShowing()) dialog.cancel();
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
