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
    switch (Server.mainInputStream.readByte()) {
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
        handleChangeSizeEvent();
        break;
      case 6:
        handleRotateEvent();
        break;
    }
  }

  private static void handleTouchEvent() throws IOException {
    int action = Server.mainInputStream.readByte();
    int pointerId = Server.mainInputStream.readByte();
    float x = Server.mainInputStream.readFloat();
    float y = Server.mainInputStream.readFloat();
    int offsetTime = Server.mainInputStream.readInt();
    Device.touchEvent(action, x, y, pointerId, offsetTime);
  }

  private static void handleKeyEvent() throws IOException {
    int keyCode = Server.mainInputStream.readInt();
    int meta = Server.mainInputStream.readInt();
    Device.keyEvent(keyCode,meta);
  }

  private static void handleClipboardEvent() throws IOException {
    int size = Server.mainInputStream.readInt();
    byte[] textBytes = new byte[size];
    Server.mainInputStream.readFully(textBytes);
    String text = new String(textBytes, StandardCharsets.UTF_8);
    Device.setClipboardText(text);
  }

  public static long lastKeepAliveTime = System.currentTimeMillis();

  private static void handleKeepAliveEvent() {
    lastKeepAliveTime = System.currentTimeMillis();
  }

  private static void handleChangeSizeEvent() throws IOException, InterruptedException {
    Device.changeDeviceSize(Server.mainInputStream.readFloat());
  }

  private static void handleRotateEvent() throws IOException {
    Device.rotateDevice(Server.mainInputStream.readByte());
  }

  public static void checkScreenOff(boolean turnOn) throws IOException, InterruptedException {
    String output = Device.execReadOutput("dumpsys deviceidle | grep mScreenOn");
    Boolean isScreenOn = null;
    if (output.contains("mScreenOn=true")) isScreenOn = true;
    else if (output.contains("mScreenOn=false")) isScreenOn = false;
    // 如果屏幕状态和要求状态不同，则模拟按下电源键
    if (isScreenOn != null && isScreenOn ^ turnOn) Device.keyEvent(26,0);
  }

}

