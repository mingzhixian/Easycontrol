package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static top.saymzx.easycontrol.app.MasterActivityKt.appData;

import android.content.ClipData;

import java.nio.ByteBuffer;
import java.util.Objects;

import kotlin.Pair;

public class Controller {
  private final Client client;

  public Controller(Client c) {
    client = c;
  }

  public Thread handle() {
    Thread thread = new HandleThread();
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  }

  class HandleThread extends Thread {
    @Override
    public void run() {
      try {
        while (client.isNormal) {
          switch (client.controlStream.readByte()) {
            case 20:
              handleClipboardEvent();
              break;
            case 21:
              handleRotationNotification();
              break;
          }
        }
      } catch (Exception ignored) {
        client.isNormal = false;
      }
    }
  }

  // 处理剪切板
  public String nowClipboardText = "";

  private void handleClipboardEvent() {
    String newClipBoardText = new String(client.controlStream.readByteArray(client.controlStream.readInt()));
    if (!Objects.equals(nowClipboardText, newClipBoardText)) {
      nowClipboardText = newClipBoardText;
      appData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, nowClipboardText));
    }
  }

  // 处理旋转
  private void handleRotationNotification() {
    client.videoDecode.videoSize = new Pair<>(client.videoDecode.videoSize.getSecond(), client.videoDecode.videoSize.getFirst());
    client.videoDecode.devicePortrait = !client.videoDecode.devicePortrait;
    client.handleUi.handle(HandleUi.ChangeRotation, 1);
  }

  // 发送触摸事件
  public void sendTouchEvent(int action, int p, int x, int y, int floatVideoWidth, int floatVideoHeight) {
    ByteBuffer touchByteBuffer = ByteBuffer.allocate(17);
    // 触摸事件
    touchByteBuffer.put((byte) 0);
    // 触摸类型
    touchByteBuffer.putInt(action);
    // pointerId
    touchByteBuffer.putInt(p);
    // 坐标位置
    touchByteBuffer.putInt(x * client.videoDecode.videoSize.getFirst() / floatVideoWidth);
    touchByteBuffer.putInt(y * client.videoDecode.videoSize.getSecond() / floatVideoHeight);
    touchByteBuffer.flip();
    client.controlStream.write(touchByteBuffer.array());
  }

  // 发送按键事件
  public void sendKeyEvent(int key) {
    ByteBuffer navByteBuffer = ByteBuffer.allocate(5);
    // 输入事件
    navByteBuffer.put((byte) 1);
    // 按键类型
    navByteBuffer.putInt(key);
    navByteBuffer.flip();
    client.controlStream.write(navByteBuffer.array());
  }

  // 发送剪切板事件
  public void sendClipboardEvent() {
    byte[] tmpTextByte = nowClipboardText.getBytes();
    if (tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 2);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.flip();
    client.controlStream.write(byteBuffer.array());
    client.controlStream.write(tmpTextByte);
  }

  // 发送屏幕控制事件
  public void sendScreenModeEvent(int mode) {
    ByteBuffer navByteBuffer = ByteBuffer.allocate(5);
    // 输入事件
    navByteBuffer.put((byte) 3);
    // 按键类型
    navByteBuffer.putInt(mode);
    navByteBuffer.flip();
    client.controlStream.write(navByteBuffer.array());
  }
}
