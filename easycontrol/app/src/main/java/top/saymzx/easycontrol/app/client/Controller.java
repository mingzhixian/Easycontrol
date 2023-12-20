package top.saymzx.easycontrol.app.client;

import android.content.ClipData;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class Controller {
  private final PublicTools.MyFunctionBytes write;

  public Controller(PublicTools.MyFunctionBytes write) {
    this.write = write;
  }

  // 剪切板
  public String nowClipboardText = "";

  public void checkClipBoard() {
    ClipData clipBoard = AppData.clipBoard.getPrimaryClip();
    if (clipBoard != null && clipBoard.getItemCount() > 0) {
      String newClipBoardText = String.valueOf(clipBoard.getItemAt(0).getText());
      if (!Objects.equals(nowClipboardText, newClipBoardText)) {
        nowClipboardText = newClipBoardText;
        sendClipboardEvent();
      }
    }
  }

  // 发送触摸事件
  public void sendTouchEvent(int action, int p, float x, float y, int offsetTime) {
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
    write.run(byteBuffer.array());
  }

  // 发送按键事件
  public void sendKeyEvent(int key, int meta) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    // 输入事件
    byteBuffer.put((byte) 2);
    // 按键类型
    byteBuffer.putInt(key);
    byteBuffer.putInt(meta);
    byteBuffer.flip();
    write.run(byteBuffer.array());
  }

  // 发送剪切板事件
  private void sendClipboardEvent() {
    byte[] tmpTextByte = nowClipboardText.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length == 0 || tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
    byteBuffer.put((byte) 3);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.put(tmpTextByte);
    byteBuffer.flip();
    write.run(byteBuffer.array());
  }

  // 发送心跳包
  public void sendKeepAlive() {
    write.run(new byte[]{4});
  }

  // 发送修改分辨率事件
  public void sendChangeSizeEvent(float newSize) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 5);
    byteBuffer.putFloat(newSize);
    byteBuffer.flip();
    write.run(byteBuffer.array());
  }

  // 发送旋转请求事件
  public void sendRotateEvent(boolean isPortrait) {
    write.run(new byte[]{6, (byte) (isPortrait ? 0 : 1)});
  }

}
