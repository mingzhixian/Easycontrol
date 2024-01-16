/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.system.ErrnoException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class ControlPacket {

  public static void sendVideoEvent(int size, long pts, ByteBuffer data) throws IOException, ErrnoException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(13 + size);
    byteBuffer.put((byte) 1);
    byteBuffer.putInt(size);
    byteBuffer.put(data);
    byteBuffer.putLong(pts);
    byteBuffer.flip();
    Server.write(byteBuffer);
  }

  public static void sendAudioEvent(int size, ByteBuffer data) throws IOException, ErrnoException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + size);
    byteBuffer.put((byte) 2);
    byteBuffer.putInt(size);
    byteBuffer.put(data);
    byteBuffer.flip();
    Server.write(byteBuffer);
  }

  public static void sendClipboardEvent(String newClipboardText) {
    byte[] tmpTextByte = newClipboardText.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length == 0 || tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
    byteBuffer.put((byte) 3);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.put(tmpTextByte);
    byteBuffer.flip();
    try {
      Server.write(byteBuffer);
    } catch (IOException | ErrnoException e) {
      Server.errorClose(e);
    }
  }

  public static void sendVideoSizeEvent() throws IOException, ErrnoException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    byteBuffer.put((byte) 4);
    byteBuffer.putInt(Device.videoSize.first);
    byteBuffer.putInt(Device.videoSize.second);
    byteBuffer.flip();
    Server.write(byteBuffer);
  }

  public static void handleTouchEvent() throws IOException {
    int action = Server.inputStream.readByte();
    int pointerId = Server.inputStream.readByte();
    float x = Server.inputStream.readFloat();
    float y = Server.inputStream.readFloat();
    int offsetTime = Server.inputStream.readInt();
    Device.touchEvent(action, x, y, pointerId, offsetTime);
  }

  public static void handleKeyEvent() throws IOException {
    int keyCode = Server.inputStream.readInt();
    int meta = Server.inputStream.readInt();
    Device.keyEvent(keyCode, meta);
  }

  public static void handleClipboardEvent() throws IOException {
    int size = Server.inputStream.readInt();
    byte[] textBytes = new byte[size];
    Server.inputStream.readFully(textBytes);
    String text = new String(textBytes, StandardCharsets.UTF_8);
    Device.setClipboardText(text);
  }

}

