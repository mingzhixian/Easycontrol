package top.saymzx.easycontrol.server.helper;

import android.util.Pair;

import java.io.IOException;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class Controller {

  public static Thread handle() {
    Thread thread = new HandleThread();
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  }

  static class HandleThread extends Thread {
    @Override
    public void run() {
      try {
        while (Server.isNormal) {
          switch (Server.controlStreamIn.readByte()) {
            case 0:
              handleTouchEvent();
              break;
            case 1:
              handleKeyEvent();
              break;
            case 2:
              handleClipboardEvent();
              break;
            case 3:
              handleSetScreenPowerModeEvent();
              break;
          }
        }
      } catch (IOException ignored) {
        Server.isNormal = false;
      }
    }
  }

  private static void handleTouchEvent() throws IOException {
    int action = Server.controlStreamIn.readInt();
    int pointerId = Server.controlStreamIn.readInt();
    Pair<Integer, Integer> position = new Pair<>(Server.controlStreamIn.readInt(), Server.controlStreamIn.readInt());
    Device.touchEvent(action, position, pointerId);
  }

  private static void handleKeyEvent() throws IOException {
    int keyCode = Server.controlStreamIn.readInt();
    Device.keyEvent(keyCode);
  }

  private static void handleClipboardEvent() throws IOException {
    int size = Server.controlStreamIn.readInt();
    byte[] textBytes = new byte[size];
    Server.controlStreamIn.readFully(textBytes, 0, size);
    String text = new String(textBytes);
    Device.setClipboardText(text);
  }

  private static void handleSetScreenPowerModeEvent() throws IOException {
    Device.setScreenPowerMode(Server.controlStreamIn.readByte());
  }
}

