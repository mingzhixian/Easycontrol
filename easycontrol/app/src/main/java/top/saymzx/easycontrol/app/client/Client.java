package top.saymzx.easycontrol.app.client;

import static top.saymzx.easycontrol.app.MasterActivityKt.appData;

import android.content.ClipData;
import android.view.Surface;
import android.widget.Toast;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kotlin.Pair;
import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.Device;

public class Client extends Thread {
  public final Device device;
  public final Surface surface;
  public final HandleUi handleUi;
  public boolean isNormal;

  // 连接流
  private Adb adb;
  public AdbStream videoStream;
  public AdbStream audioStream;
  public AdbStream controlStream;

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
    if (isNormal) return;
    // 发送server
    sendServer();
    if (isNormal) return;
    // 启动server
    startServer();
    if (isNormal) return;
    // 连接server
    connectServer();
    if (isNormal) return;
    // 创建子服务
    createSubService();
    if (isNormal) return;
    // 启动子服务
    startSubService();
    // 更新UI
    handleUi.handle(HandleUi.StartControl, 0);
    // 其他服务
    startOtherService();
  }

  private void stop(String de, Exception e) {
    // 更新UI
    handleUi.handle(HandleUi.StopControl, 0);
    Toast.makeText(appData.main, de, Toast.LENGTH_SHORT).show();
    if (e != null) Toast.makeText(appData.main, e.toString(), Toast.LENGTH_SHORT).show();
    // 关闭资源
    for (int i = 0; i < 8; i++) {
      try {
        switch (i) {
          case 1:
            videoStream.close();
          case 2:
            audioStream.close();
          case 3:
            controlStream.close();
          case 4:
            videoDecode.videoDecodec.stop();
            videoDecode.videoDecodec.release();
          case 5:
            audioDecode.audioTrack.stop();
            audioDecode.audioTrack.release();
          case 6:
            audioDecode.loudnessEnhancer.release();
          case 7:
            audioDecode.audioDecodec.stop();
            audioDecode.audioDecodec.release();
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
    if (ip == null) {
      stop("地址错误", null);
      return;
    }
    try {
      // 连接ADB
      adb = new Adb(ip, device.getPort(), appData.keyPair);
    } catch (Exception e) {
      stop("连接ADB失败", e);
    }
  }

  // 发送Server
  private void sendServer() {
    // 尝试发送Server
    try {
      for (int i = 0; i < 3; i++) {
        String isHaveServer = runAdbCmd(" ls -l /data/local/tmp/easycontrol_server_${appData.versionCode}.jar ", true);
        if (isHaveServer.contains("命令运行超过8秒，未获取命令输出")) continue;
        else if (isHaveServer.contains("No such file or directory") || isHaveServer.contains("Invalid argument")) {
          // 删除旧Server
          runAdbCmd("rm /data/local/tmp/easycontrol_server_* ", false);
          adb.pushFile(appData.main.getResources().openRawResource(R.raw.scrcpy_server), "/data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar");
        } else {
          return;
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
      if (device.getSetResolution())
        runAdbCmd("wm size ${appData.deviceWidth}x${appData.deviceHeight}", false);
      // 停止旧服务
      runAdbCmd("ps -ef | grep top.saymzx.easycontrol.server.Server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9", false);
      // 启动Server
      runAdbCmd("CLASSPATH=/data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar app_process / top.saymzx.easycontrol.server.Server video_codec=${device.videoCodec} audio_codec=${device.audioCodec} max_size=${device.maxSize} video_bit_rate=${device.videoBit} max_fps=${device.fps} > /dev/null 2>&1 & ", false);
      // 检查启动状态
      String out = runAdbCmd("ps -ef | grep top.saymzx.easycontrol.server.Server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o", true);
      if (!out.isEmpty()) return;
    } catch (Exception ignored) {
    }
    stop("启动Server失败", null);
  }

  // 连接Server
  private void connectServer() {
    // 尝试连接
    int connect = 0;
    for (int i = 0; i < 100; i++) {
      try {
        if (connect == 0) {
          videoStream = adb.localSocketForward("scrcpy_android", true);
          connect = 1;
        }
        if (connect == 1) {
          audioStream = adb.localSocketForward("scrcpy_android", true);
          connect = 2;
        }
        controlStream = adb.localSocketForward("scrcpy_android", true);
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
      Pair<Thread, Thread> videoThreads = videoDecode.stream();
      workThreads.add(videoThreads.getFirst());
      workThreads.add(videoThreads.getSecond());
      Pair<Thread, Thread> audioThreads = audioDecode.stream();
      workThreads.add(audioThreads.getFirst());
      workThreads.add(audioThreads.getSecond());
      workThreads.add(controller.handle());
      for (Thread thread : workThreads) {
        if (thread != null) thread.start();
      }
    } catch (Exception e) {
      stop("启动Client失败", e);
    }
  }

  // 其余服务
  private void startOtherService() {
    while (isNormal) {
      try {
        Thread.sleep(500);
        // 更新刷新率
        handleUi.handle(HandleUi.UpdateFps, videoDecode.fps * 2);
        videoDecode.fps = 0;
        // 更新延迟
        handleUi.handle(HandleUi.UpdateDelay, videoDecode.avgDelay / 60);
        // 熄屏检测
        checkScreenOff();
        // 剪切板检测
        checkClipBoard();
      } catch (Exception ignored) {
        stop("运行错误", null);
      }
    }
  }

  // 防止被控端熄屏
  private long lastSendCmdTime = 0;

  private void checkScreenOff() {
    long nowTime = System.currentTimeMillis();
    if (nowTime - lastSendCmdTime > 1000) {
      if (runAdbCmd("dumpsys deviceidle | grep mScreenOn", true).contains("mScreenOn=false")) {
        lastSendCmdTime = nowTime;
        runAdbCmd("input keyevent 26", false);
        if (appData.getSetValue().getSlaveTurnOffScreen()) {
          controller.sendScreenModeEvent(0);
        }
      }
    }
  }

  // 同步本机剪切板至被控端
  private void checkClipBoard() {
    ClipData clipBoard = appData.clipBoard.getPrimaryClip();
    if (clipBoard != null && clipBoard.getItemCount() > 0) {
      String newClipBoardText = clipBoard.getItemAt(0).getText().toString();
      if (!Objects.equals(controller.nowClipboardText, newClipBoardText)) {
        controller.nowClipboardText = newClipBoardText;
        controller.sendClipboardEvent();
      }
    }
  }

  // 运行ADB命令
  private String runAdbCmd(String cmd, Boolean isNeedOutput) {
    String out;
    try {
      out = adb.runAdbCmd(cmd, isNeedOutput);
    } catch (Exception ignored) {
      out = "";
    }
    return out;
  }
}
