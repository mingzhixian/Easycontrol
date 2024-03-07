package top.saymzx.easycontrol.app.client.tools;

import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ControlPacket {

  // 触摸事件
  public static ByteBuffer createTouchEvent(int action, int p, float x, float y, int offsetTime) {
    if (x < 0 || x > 1 || y < 0 || y > 1) {
      // 超出范围则改为抬起事件
      if (x < 0) x = 0;
      if (x > 1) x = 1;
      if (y < 0) y = 0;
      if (y > 1) y = 1;
      action = MotionEvent.ACTION_UP;
    }
    ByteBuffer byteBuffer = ByteBuffer.allocate(15);
    // 触摸事件
    byteBuffer.put((byte) 1);
    // 触摸类型
    byteBuffer.put((byte) action);
    // pointerId
    byteBuffer.put((byte) p);
    // 坐标位置
    byteBuffer.putFloat(x);
    byteBuffer.putFloat(y);
    // 时间偏移
    byteBuffer.putInt(offsetTime);
    byteBuffer.flip();
    return byteBuffer;
  }

  // 按键事件
  public static ByteBuffer createKeyEvent(int key, int meta) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    // 输入事件
    byteBuffer.put((byte) 2);
    // 按键类型
    byteBuffer.putInt(key);
    byteBuffer.putInt(meta);
    byteBuffer.flip();
    return byteBuffer;
  }

  // 剪切板事件
  public static ByteBuffer createClipboardEvent(String text) {
    byte[] tmpTextByte = text.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length == 0 || tmpTextByte.length > 5000) return null;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
    byteBuffer.put((byte) 3);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.put(tmpTextByte);
    byteBuffer.flip();
    return byteBuffer;
  }

  // 心跳包
  public static ByteBuffer createKeepAlive() {
    return ByteBuffer.wrap(new byte[]{4});
  }

  // 修改分辨率事件
  public static ByteBuffer createChangeResolutionEvent(float newSize) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 5);
    byteBuffer.putFloat(newSize);
    byteBuffer.flip();
    return byteBuffer;
  }

  // 修改分辨率事件
  public static ByteBuffer createChangeResolutionEvent(int width, int height) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    byteBuffer.put((byte) 9);
    byteBuffer.putInt(width);
    byteBuffer.putInt(height);
    byteBuffer.flip();
    return byteBuffer;
  }

  // 旋转请求事件
  public static ByteBuffer createRotateEvent() {
    return ByteBuffer.wrap(new byte[]{6});
  }

  // 背光控制事件
  public static ByteBuffer createLightEvent(int mode) {
    return ByteBuffer.wrap(new byte[]{7, (byte) mode});
  }

  // 电源键事件
  public static ByteBuffer createPowerEvent(int mode) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 8);
    byteBuffer.putInt(mode);
    byteBuffer.flip();
    return byteBuffer;
  }

}
