package top.saymzx.easycontrol.app.client;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.view.ClientView;
import top.saymzx.easycontrol.app.client.view.FullActivity;
import top.saymzx.easycontrol.app.client.view.SmallView;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class Client {
  public static final ArrayList<Client> allClients = new ArrayList<>();

  // 连接，双连接，避免tcp对头阻塞，以及避免adb同步阻塞
  private Adb adb;
  private Adb videoAdb;
  private AdbStream stream;
  private AdbStream videoStream;

  // 子服务
  public VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public Controller controller;
  public final ClientView clientView;
  private final Dialog dialog = PublicTools.createClientLoading(AppData.main, () -> this.errorClose(null));

  private final ArrayList<Thread> threads = new ArrayList<>();
  private static final int timeoutDelay = 1000 * 10;

  public Client(Device device, UsbDevice usbDevice) {
    allClients.add(this);
    clientView = new ClientView(this, device.setResolution);
    // 显示加载框
    dialog.show();
    // 启动超时
    threads.add(new Thread(() -> {
      try {
        Thread.sleep(timeoutDelay);
        errorClose(new Exception("连接超时"));
      } catch (InterruptedException ignored) {
      }
    }));
    threads.get(0).start();
    // 启动Client
    new Thread(() -> {
      try {
        // 连接ADB
        connectADB(device, usbDevice);
        // 发送Server
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
      } catch (Exception e) {
        errorClose(e);
      }
    }).start();
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
    if (BuildConfig.ENABLE_DEBUG_FEATURE) {
      adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/" + AppData.serverName);
    } else {
      for (int i = 0; i < 3; i++) {
        String isHaveServer = adb.runAdbCmd("ls -l /data/local/tmp/easycontrol_*", true);
        if (isHaveServer.contains(AppData.serverName)) return;
        adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/" + AppData.serverName);
      }
      throw new Exception("发送Server失败");
    }
  }

  // 启动Server
  private void startServer(Device device) throws IOException, InterruptedException {
    float reSize = device.setResolution ? (device.defaultFull ? FullActivity.getResolution() : SmallView.getResolution()) : -1;
    adb.runAdbCmd("app_process -Djava.class.path=/data/local/tmp/" + AppData.serverName + " / top.saymzx.easycontrol.server.Server" + " is_audio=" + (device.isAudio ? 1 : 0) + " max_size=" + device.maxSize + " max_fps=" + device.maxFps + " video_bit_rate=" + device.maxVideoBit + " turn_off_screen=" + (device.turnOffScreen ? 1 : 0) + " auto_control_screen=" + (device.autoControlScreen ? 1 : 0) + " reSize=" + reSize + " is_h265_decoder_support=" + ((AppData.setting.getDefaultH265() && PublicTools.isH265DecoderSupport()) ? 1 : 0) + " > /dev/null 2>&1 &", false);
  }

  // 连接Server
  private void connectServer() throws Exception {
    Thread.sleep(100);
    // 尝试连接
    for (int i = 0; i < 40; i++) {
      try {
        if (stream == null) stream = adb.localSocketForward("easycontrol", true);
        if (videoStream == null) videoStream = videoAdb.localSocketForward("easycontrol", true);
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception("连接Server失败");
  }

  // 创建子服务
  private void createSubService() throws IOException, InterruptedException {
    // 是否支持H265编码
    boolean isH265Support = videoStream.readByte() == 1;
    // 视频大小
    if (stream.readByte() != CHANGE_SIZE_EVENT) throw new IOException("启动Client失败:数据错误");
    Pair<Integer, Integer> newVideoSize = new Pair<>(stream.readInt(), stream.readInt());
    clientView.updateVideoSize(newVideoSize);
    // 视频解码
    Pair<Long, ByteBuffer> csd0 = readFrame(videoStream);
    videoDecode = new VideoDecode(newVideoSize, csd0, isH265Support ? null : readFrame(videoStream));
    // 音频解码
    if (stream.readByte() == 1) {
      if (stream.readByte() != AUDIO_EVENT) throw new IOException("启动Client失败:数据错误");
      csd0 = readFrame(stream);
      audioDecode = new AudioDecode(csd0);
    }
    // 控制
    controller = new Controller(clientView, stream);
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
    threads.get(0).interrupt();
    threads.clear();
    threads.add(new Thread(this::executeVideoDecodeIn));
    threads.add(new Thread(this::executeVideoDecodeOut));
    if (audioDecode != null) threads.add(new Thread(this::executeAudioDecodeOut));
    threads.add(new Thread(this::executeStreamIn));
    for (Thread thread : threads) thread.setPriority(Thread.MAX_PRIORITY);
    threads.add(new Thread(this::executeOtherService));
    for (Thread thread : threads) thread.start();
  }

  // 服务分发
  private static final int AUDIO_EVENT = 1;
  private static final int CLIPBOARD_EVENT = 2;
  private static final int CHANGE_SIZE_EVENT = 3;

  private void executeStreamIn() {
    try {
      while (!Thread.interrupted()) {
        switch (stream.readByte()) {
          case AUDIO_EVENT:
            Pair<Long, ByteBuffer> audioFrame = readFrame(stream);
            if (clientView.checkIsNeedPlay()) audioDecode.decodeIn(audioFrame);
            break;
          case CLIPBOARD_EVENT:
            controller.handleClipboardEvent();
            break;
          case CHANGE_SIZE_EVENT:
            controller.handleChangeSizeEvent();
            break;
        }
      }
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private void executeVideoDecodeIn() {
    try {
      while (!Thread.interrupted()) {
        videoAdb.sendMoreOk(videoStream);
        videoDecode.decodeIn(readFrame(videoStream));
        videoAdb.sendMoreOk(videoStream);
      }
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private void executeVideoDecodeOut() {
    try {
      while (!Thread.interrupted()) videoDecode.decodeOut(clientView.checkIsNeedPlay());
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private void executeAudioDecodeOut() {
    try {
      while (!Thread.interrupted()) audioDecode.decodeOut();
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private void executeOtherService() {
    try {
      while (!Thread.interrupted()) {
        controller.checkClipBoard();
        controller.sendKeepAlive();
        checkScreenRotation();
        // 背景模糊效果，耗费性能，且因更新频率不高导致效果不是很好
//        AppData.main.runOnUiThread(clientView::updateBackImage);
        Thread.sleep(1500);
      }
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private Boolean lastScreenIsPortal = null;

  private void checkScreenRotation() {
    Pair<Integer, Integer> screenSize = PublicTools.getScreenSize();
    boolean nowScreenIsPortal = screenSize.first < screenSize.second;
    if (lastScreenIsPortal != null && lastScreenIsPortal != nowScreenIsPortal) AppData.main.runOnUiThread(() -> clientView.hasChangeRotation(screenSize));
    lastScreenIsPortal = nowScreenIsPortal;
  }

  private boolean hasClose = false;

  private void errorClose(Exception error) {
    if (hasClose) return;
    if (error != null) {
      String errorStr = String.valueOf(error);
      Log.e("Easycontrol", errorStr);
      AppData.main.runOnUiThread(() -> Toast.makeText(AppData.main, errorStr, Toast.LENGTH_SHORT).show());
    }
    AppData.main.runOnUiThread(() -> clientView.hide(true));
  }

  private Pair<Long, ByteBuffer> readFrame(AdbStream stream) throws InterruptedException, IOException {
    long pts = stream.readLong();
    return new Pair<>(pts, stream.readByteArray(stream.readInt()));
  }

  public void release() {
    if (hasClose) return;
    hasClose = true;
    if (dialog.isShowing()) dialog.cancel();
    for (Thread thread : threads) thread.interrupt();
    if (adb != null) adb.close();
    if (videoAdb != null) videoAdb.close();
    if (videoDecode != null) videoDecode.release();
    if (audioDecode != null) audioDecode.release();
    allClients.remove(this);
  }

}
