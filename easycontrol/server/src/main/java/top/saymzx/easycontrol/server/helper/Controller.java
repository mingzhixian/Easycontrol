/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class Controller {

  public static void handleIn() throws IOException, InterruptedException {
    switch (Server.inputStream.readByte()) {
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
        handleChangeSizeEvent();
        break;
      case 7:
        handleRotateEvent();
        break;
    }
  }

  private static void handleTouchEvent() throws IOException {
    int action = Server.inputStream.readByte();
    int pointerId = Server.inputStream.readByte();
    float x = Server.inputStream.readFloat();
    float y = Server.inputStream.readFloat();
    int offsetTime = Server.inputStream.readInt();
    Device.touchEvent(action, x, y, pointerId, offsetTime);
  }

  private static void handleKeyEvent() throws IOException {
    int keyCode = Server.inputStream.readInt();
    Device.keyEvent(keyCode);
  }

  private static void handleClipboardEvent() throws IOException {
    int size = Server.inputStream.readInt();
    byte[] textBytes = new byte[size];
    Server.inputStream.readFully(textBytes);
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

  private static void handleChangeSizeEvent() throws IOException, InterruptedException {
    Device.changeDeviceSize(Server.inputStream.readFloat());
  }

  private static void handleRotateEvent() throws IOException {
    Device.rotateDevice(Server.inputStream.readByte());
  }

  public static void checkScreenOff(boolean turnOn) throws IOException, InterruptedException {
    String output = Device.execReadOutput("dumpsys deviceidle | grep mScreenOn");
    Boolean isScreenOn = null;
    if (output.contains("mScreenOn=true")) isScreenOn = true;
    else if (output.contains("mScreenOn=false")) isScreenOn = false;
    // 如果屏幕状态和要求状态不同，则模拟按下电源键
    if (isScreenOn != null && isScreenOn ^ turnOn) Device.keyEvent(26);
  }

}

