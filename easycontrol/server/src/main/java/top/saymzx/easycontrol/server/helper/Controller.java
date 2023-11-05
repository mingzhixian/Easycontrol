/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class Controller {

  public static void handleIn() throws IOException {
    boolean hasData = true;
    while (hasData) {
      switch (Server.streamIn.readByte()) {
        case 1:
          handleTouchEvent();
          break;
        case 2:
          handleKeyEvent();
          break;
        case 3:
          handleClipboardEvent();
          break;
        case 4:
          handleKeepAliveEvent();
          break;
        case 5:
          handlePowerEvent();
          break;
        case 6:
          handleCongestionEvent();
          break;
      }
      hasData = Server.streamIn.available() > 0;
    }
  }

  private static void handleTouchEvent() throws IOException {
    int action = Server.streamIn.readByte();
    int pointerId = Server.streamIn.readByte();
    float x = Server.streamIn.readFloat();
    float y = Server.streamIn.readFloat();
    Device.touchEvent(action, x, y, pointerId);
  }

  private static void handleKeyEvent() throws IOException {
    int keyCode = Server.streamIn.readInt();
    Device.keyEvent(keyCode);
  }

  private static void handleClipboardEvent() throws IOException {
    int size = Server.streamIn.readInt();
    byte[] textBytes = new byte[size];
    Server.streamIn.readFully(textBytes);
    String text = new String(textBytes, StandardCharsets.UTF_8);
    Device.setClipboardText(text);
  }

  public static long lastKeepAliveTime = System.currentTimeMillis();

  private static void handleKeepAliveEvent() {
    lastKeepAliveTime = System.currentTimeMillis();
  }

  private static void handlePowerEvent() {
    Device.keyEvent(26);
  }

  public static final int[] maxFpsList = new int[]{60, 45, 35, 25, 15};

  private static void handleCongestionEvent() {
    for (int i : maxFpsList) {
      if (i < VideoEncode.maxFps) {
        VideoEncode.maxFps = i;
        VideoEncode.isHasChangeConfig = true;
        break;
      }
    }
  }

  public static void checkScreenOff(boolean turnOn) throws IOException {
    Process process = new ProcessBuilder().command("sh", "-c", "dumpsys deviceidle | grep mScreenOn").start();
    String output = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
    Boolean isScreenOn = null;
    if (output.contains("mScreenOn=true")) isScreenOn = true;
    else if (output.contains("mScreenOn=false")) isScreenOn = false;
    if (isScreenOn != null) {
      // 如果屏幕状态和要求状态不同，则模拟按下电源键
      if (isScreenOn ^ turnOn) Device.keyEvent(26);
    }
  }

}

