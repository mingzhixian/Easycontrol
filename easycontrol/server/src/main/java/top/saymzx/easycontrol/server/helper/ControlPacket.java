/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;

public final class ControlPacket {

  public static void sendVideoEvent(long pts, ByteBuffer data) throws IOException {
    int size = data.remaining() + 8;
    if (size < 8) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(4 + size);
    byteBuffer.putInt(size);
    byteBuffer.putLong(pts);
    byteBuffer.put(data);
    byteBuffer.flip();
    Server.writeVideo(byteBuffer);
  }

  public static void sendAudioEvent(ByteBuffer data) throws IOException {
    int size = data.remaining();
    if (size < 0) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + size);
    byteBuffer.put((byte) 1);
    byteBuffer.putInt(size);
    byteBuffer.put(data);
    byteBuffer.flip();
    Server.writeMain(byteBuffer);
  }

  public static void sendClipboardEvent(String newClipboardText) {
    byte[] tmpTextByte = newClipboardText.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length == 0 || tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
    byteBuffer.put((byte) 2);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.put(tmpTextByte);
    byteBuffer.flip();
    try {
      Server.writeMain(byteBuffer);
    } catch (IOException e) {
      Server.errorClose(e);
    }
  }

  public static void sendVideoSizeEvent() throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    byteBuffer.put((byte) 3);
    byteBuffer.putInt(Device.videoSize.first);
    byteBuffer.putInt(Device.videoSize.second);
    byteBuffer.flip();
    Server.writeMain(byteBuffer);
  }

  public static void handleTouchEvent() throws IOException {
    int action = Server.mainInputStream.readByte();
    int pointerId = Server.mainInputStream.readByte();
    float x = Server.mainInputStream.readFloat();
    float y = Server.mainInputStream.readFloat();
    int offsetTime = Server.mainInputStream.readInt();
    Device.touchEvent(action, x, y, pointerId, offsetTime);
  }

  public static void handleKeyEvent() throws IOException {
    int keyCode = Server.mainInputStream.readInt();
    int meta = Server.mainInputStream.readInt();
    Device.keyEvent(keyCode, meta);
  }

  public static void handleClipboardEvent() throws IOException {
    int size = Server.mainInputStream.readInt();
    byte[] textBytes = new byte[size];
    Server.mainInputStream.readFully(textBytes);
    String text = new String(textBytes, StandardCharsets.UTF_8);
    Device.setClipboardText(text);
  }

}

