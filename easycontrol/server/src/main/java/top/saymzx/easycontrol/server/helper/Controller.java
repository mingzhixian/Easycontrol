/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class Controller {

  public static Thread start() {
    Thread thread = new HandleThread();
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  }

  static class HandleThread extends Thread {
    @Override
    public void run() {
      try {
        while (Server.isNormal.get()) {
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
            case 4:
              handleDelayEvent();
              break;
          }
        }
      } catch (Exception ignored) {
        Server.isNormal.set(false);
      }
    }
  }

  private static void handleTouchEvent() throws IOException {
    int action = Server.controlStreamIn.readInt();
    int pointerId = Server.controlStreamIn.readInt();
    Pair<Float, Float> position = new Pair<>(Server.controlStreamIn.readFloat(), Server.controlStreamIn.readFloat());
    int vertical = Server.controlStreamIn.read();
    Device.touchEvent(action, position, pointerId, vertical);
  }

  private static void handleKeyEvent() throws IOException {
    int keyCode = Server.controlStreamIn.readInt();
    Device.keyEvent(keyCode);
  }

  private static void handleClipboardEvent() throws IOException {
    int size = Server.controlStreamIn.readInt();
    byte[] textBytes = new byte[size];
    Server.controlStreamIn.readFully(textBytes);
    String text = new String(textBytes, StandardCharsets.UTF_8);
    Device.setClipboardText(text);
  }

  private static void handleSetScreenPowerModeEvent() throws IOException {
    int mode = Server.controlStreamIn.readByte();
    Device.setScreenPowerMode(mode);
  }

  private static void handleDelayEvent() throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 22);
    byteBuffer.putInt((int) (System.currentTimeMillis() & 0x00000000FFFFFFFFL));
    byteBuffer.flip();
    synchronized (Server.controlStream) {
      Server.writeFully(Server.controlStream, byteBuffer);
    }
  }
}

