package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.util.Pair;
import android.view.MotionEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import top.saymzx.easycontrol.adb.AdbStream;
import top.saymzx.easycontrol.app.client.view.ClientView;
import top.saymzx.easycontrol.app.entity.AppData;

public class Controller {
  private final ClientView clientView;
  private final AdbStream stream;

  public Controller(ClientView clientView, AdbStream stream) {
    this.clientView = clientView;
    this.stream = stream;
  }

  // 剪切板
  private String nowClipboardText = "";

  public void checkClipBoard() {
    ClipData clipBoard = AppData.clipBoard.getPrimaryClip();
    if (clipBoard != null && clipBoard.getItemCount() > 0) {
      String newClipBoardText = clipBoard.getItemAt(0).getText().toString();
      if (!Objects.equals(nowClipboardText, newClipBoardText)) {
        nowClipboardText = newClipBoardText;
        sendClipboardEvent();
      }
    }
  }

  public void handleClipboardEvent() throws InterruptedException, IOException {
    int size = stream.readInt();
    nowClipboardText = new String(stream.readByteArray(size).array());
    AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, nowClipboardText));
  }

  // 处理画面大小变化
  public void handleChangeSizeEvent() throws IOException, InterruptedException {
    clientView.videoSize = new Pair<>(stream.readInt(), stream.readInt());
    AppData.main.runOnUiThread(() -> clientView.reCalculateTextureViewSize());
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
    writeStream(byteBuffer);
  }

  // 发送按键事件
  public void sendKeyEvent(int key) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    // 输入事件
    byteBuffer.put((byte) 2);
    // 按键类型
    byteBuffer.putInt(key);
    byteBuffer.flip();
    writeStream(byteBuffer);
  }

  // 发送剪切板事件
  private void sendClipboardEvent() {
    byte[] tmpTextByte = nowClipboardText.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
    byteBuffer.put((byte) 3);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.put(tmpTextByte);
    byteBuffer.flip();
    writeStream(byteBuffer);
  }

  // 发送心跳包
  public void sendKeepAlive() {
    writeStream(ByteBuffer.wrap(new byte[]{4}));
  }

  // 发送按键事件
  public void sendPowerEvent() {
    writeStream(ByteBuffer.wrap(new byte[]{5}));
  }

  // 发送修改分辨率事件
  public void sendChangeSizeEvent(Pair<Integer, Integer> newSize) {
    if (newSize.first < 0 || newSize.second < 0) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 6);
    byteBuffer.putFloat((float) newSize.first / (float) newSize.second);
    byteBuffer.flip();
    writeStream(byteBuffer);
  }

  private void writeStream(ByteBuffer byteBuffer) {
    try {
      stream.write(byteBuffer);
    } catch (IOException | InterruptedException ignored) {
      AppData.main.runOnUiThread(() -> clientView.hide(true));
    }
  }

}
