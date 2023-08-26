package top.saymzx.easycontrol.app.client;

import static top.saymzx.easycontrol.app.MainActivityKt.appData;

import android.view.Surface;
import android.widget.Toast;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Pair;
import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.Device;

public class Client extends Thread {
  public final Device device;
  public final Surface surface;
  public final HandleUi handleUi;
  public AtomicBoolean isNormal = new AtomicBoolean(true);

  // 连接流
  private Adb adb;
  public AdbStream videoStream = null;
  public AdbStream audioStream = null;
  public AdbStream controlStream = null;

  // 子服务
  public VideoDecode videoDecode;
  public AudioDecode audioDecode;
  public Controller controller;


  public Client(Device d, Surface s, HandleUi h) {
    device = d;
    surface = s;
    handleUi = h;
  }

  // 启动
  @Override
  public void run() {
    // 连接ADB
    connectADB();
    if (!isNormal.get()) return;
    // 发送server
    sendServer();
    if (!isNormal.get()) return;
    // 启动server
    startServer();
    if (!isNormal.get()) return;
    // 连接server
    connectServer();
    if (!isNormal.get()) return;
    // 创建子服务
    createSubService();
    if (!isNormal.get()) return;
    // 启动子服务
    startSubService();
    // 更新UI
    handleUi.handle(HandleUi.StartControl, 0);
  }

  public void stop(String de, Exception e) {
    if (!isNormal.getAndSet(false)) return;
    // 更新UI
    handleUi.handle(HandleUi.StopControl, 0);
    appData.getMain().runOnUiThread(() -> Toast.makeText(appData.getMain(), de, Toast.LENGTH_SHORT).show());
    if (e != null)
      appData.getMain().runOnUiThread(() -> Toast.makeText(appData.getMain(), e.toString(), Toast.LENGTH_SHORT).show());
    // 关闭资源
    for (int i = 0; i < 9; i++) {
      try {
        switch (i) {
          case 1:
            if (device.getSetResolution()) adb.runAdbCmd("wm size reset", false);
          case 2:
            videoStream.close();
          case 3:
            audioStream.close();
          case 4:
            controlStream.close();
          case 5:
            videoDecode.videoDecodec.stop();
            videoDecode.videoDecodec.release();
          case 6:
            audioDecode.audioTrack.stop();
            audioDecode.audioTrack.release();
            audioDecode.loudnessEnhancer.release();
          case 7:
            audioDecode.audioDecodec.stop();
            audioDecode.audioDecodec.release();
          case 8:
            adb.close();
        }
      } catch (Exception ignored) {
      }
    }
  }

  // 连接ADB
  private void connectADB() {
    String ip;
    try {
      // 获取IP地址
      ip = Inet4Address.getByName(device.getAddress()).getHostAddress();
    } catch (Exception e) {
      stop("解析域名失败", e);
      return;
    }
    try {
      // 连接ADB
      assert ip != null;
      adb = new Adb(ip, device.getPort(), appData.getKeyPair());
    } catch (Exception e) {
      stop("连接ADB失败", e);
    }
  }

  // 发送Server
  private void sendServer() {
    // 尝试发送Server
    try {
      for (int i = 0; i < 3; i++) {
        String isHaveServer = runAdbCmd(" ls -l /data/local/tmp/", true);
        if (isHaveServer.contains("easycontrol_server_" + appData.getVersionCode() + ".jar"))
          return;
        else {
          // 删除旧Server
          runAdbCmd("rm /data/local/tmp/easycontrol_server_* ", false);
          adb.pushFile(appData.getMain().getResources().openRawResource(R.raw.easycontrol_server), "/data/local/tmp/easycontrol_server_" + appData.getVersionCode() + ".jar");
        }
      }
    } catch (Exception ignored) {
    }
    stop("发送Server失败", null);
  }

  // 启动Server
  private void startServer() {
    try {
      // 修改分辨率
      if (device.getSetResolution()) {
        Pair<Integer, Integer> tmpDeviceSize = appData.getPublicTools().getScreenSize(appData.getMain());
        runAdbCmd("wm size " + tmpDeviceSize.getFirst() + "x" + tmpDeviceSize.getSecond(), false);
      }
      // 停止旧服务
      runAdbCmd("ps -ef | grep top.saymzx.easycontrol.server.Server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9", false);
      // 启动Server
      runAdbCmd("CLASSPATH=/data/local/tmp/easycontrol_server_" + appData.getVersionCode() + ".jar app_process / top.saymzx.easycontrol.server.Server video_codec=" + device.getVideoCodec() + " audio_codec=" + device.getAudioCodec() + " max_size=" + device.getMaxSize() + " max_fps=" + device.getMaxFps() + " video_bit_rate=" + device.getMaxVideoBit() + "  > /dev/null 2>&1 & ", false);
      return;
    } catch (Exception ignored) {
    }
    stop("启动Server失败", null);
  }

  // 连接Server
  private void connectServer() {
    // 尝试连接
    for (int i = 0; i < 100; i++) {
      try {
        if (videoStream == null) videoStream = adb.localSocketForward("easycontrol", true);
        if (audioStream == null) audioStream = adb.localSocketForward("easycontrol", true);
        if (controlStream == null) controlStream = adb.localSocketForward("easycontrol", true);
        return;
      } catch (Exception ignored) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored1) {
        }
      }
    }
    // 连接失败
    stop("连接Server失败", null);
  }

  // 创建子服务
  private void createSubService() {
    try {
      videoDecode = new VideoDecode(this);
      audioDecode = new AudioDecode(this);
      controller = new Controller(this);
    } catch (Exception e) {
      stop("启动Client失败", e);
    }
  }

  // 启动子服务
  private void startSubService() {
    try {
      List<Thread> workThreads = new ArrayList<>();
      Pair<Thread, Thread> videoThreads = videoDecode.start();
      workThreads.add(videoThreads.getFirst());
      workThreads.add(videoThreads.getSecond());
      Pair<Thread, Thread> audioThreads = audioDecode.start();
      workThreads.add(audioThreads.getFirst());
      workThreads.add(audioThreads.getSecond());
      workThreads.add(controller.start());
      for (Thread thread : workThreads) {
        if (thread != null) thread.start();
      }
      controller.startHandleInThread();
    } catch (Exception e) {
      stop("启动Client失败", e);
    }
  }

  // 运行ADB命令
  public String runAdbCmd(String cmd, Boolean isNeedOutput) {
    String out;
    try {
      out = adb.runAdbCmd(cmd, isNeedOutput);
    } catch (Exception ignored) {
      out = "";
    }
    return out;
  }
}
