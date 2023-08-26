package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static top.saymzx.easycontrol.app.MainActivityKt.appData;

import android.content.ClipData;
import android.os.Handler;
import android.os.HandlerThread;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import kotlin.Pair;
import okio.Buffer;

public class Controller {
  private final Client client;
  private HandlerThread handleInThread;
  private Handler handler;

  public Controller(Client c) {
    client = c;
  }

  public Thread start() {
    handleInThread = new HandlerThread("HandleInThread");
    handleInThread.start();
    handler = new Handler(handleInThread.getLooper());
    Thread handleOutThread = new HandleOutThread();
    handleOutThread.setPriority(Thread.MAX_PRIORITY);
    return handleOutThread;
  }

  public void startHandleInThread() {
    handler.post(this::checkControlIn);
    handler.post(this::otherService);
  }

  private void checkControlIn() {
    try {
      if (client.isNormal.get() && client.controlStream.getSource().request(1)) {
        switch (client.controlStream.readByte()) {
          case 20:
            handleClipboardEvent();
            break;
          case 21:
            handleRotationNotification();
            break;
        }
      }
      // 下次200毫秒后运行
      handler.postDelayed(this::checkControlIn, 200);
    } catch (Exception ignored) {
      handleInThread.quit();
      client.stop("连接中断", null);
    }
  }

  private void otherService() {
    if (client.isNormal.get()) {
      try {
        // 更新刷新率
        client.handleUi.handle(HandleUi.UpdateFps, client.videoDecode.fps);
        client.videoDecode.fps = 0;
        // 更新延迟
        client.handleUi.handle(HandleUi.UpdateDelay, client.videoDecode.avgDelay / 60);
        // 熄屏检测
        checkScreenOff();
        // 剪切板检测
        checkClipBoard();
        // 下次1秒后运行
        handler.postDelayed(this::otherService, 1000);
      } catch (Exception ignored) {
        handleInThread.quit();
        client.stop("运行错误", null);
      }
    }
  }

  // 防止被控端熄屏
  private long lastSendCmdTime = 0;

  private void checkScreenOff() {
    long nowTime = System.currentTimeMillis();
    if (nowTime - lastSendCmdTime > 1000) {
      if (client.runAdbCmd("dumpsys deviceidle | grep mScreenOn", true).contains("mScreenOn=false")) {
        lastSendCmdTime = nowTime;
        client.runAdbCmd("input keyevent 26", false);
        if (appData.getSetting().getSlaveTurnOffScreen()) {
          sendScreenModeEvent(0);
        }
      }
    }
  }

  // 同步本机剪切板至被控端
  private void checkClipBoard() {
    ClipData clipBoard = appData.getClipBoard().getPrimaryClip();
    if (clipBoard != null && clipBoard.getItemCount() > 0) {
      String newClipBoardText = clipBoard.getItemAt(0).getText().toString();
      if (!Objects.equals(nowClipboardText, newClipBoardText)) {
        nowClipboardText = newClipBoardText;
        sendClipboardEvent();
      }
    }
  }

  private final Buffer controlOutBuffer = new Buffer();

  class HandleOutThread extends Thread {
    @Override
    public void run() {
      try {
        while (client.isNormal.get()) {
          if (!controlOutBuffer.request(1)) synchronized (controlOutBuffer) {
            controlOutBuffer.wait();
          }
          client.controlStream.write(controlOutBuffer.readByteArray());
        }
      } catch (Exception ignored) {
        client.stop("连接中断", null);
      }
    }
  }

  // 处理剪切板
  public String nowClipboardText = "";

  private void handleClipboardEvent() {
    nowClipboardText = new String(client.controlStream.readByteArray(client.controlStream.readInt()), StandardCharsets.UTF_8);
    appData.getClipBoard().setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, nowClipboardText));
  }

  // 处理旋转
  private void handleRotationNotification() {
    client.videoDecode.videoSize = new Pair<>(client.videoDecode.videoSize.getSecond(), client.videoDecode.videoSize.getFirst());
    client.videoDecode.videoPortrait = !client.videoDecode.videoPortrait;
    client.handleUi.handle(HandleUi.ChangeRotation, 1);
  }

  // 发送触摸事件
  public void sendTouchEvent(int action, int p, int x, int y, int floatVideoWidth, int floatVideoHeight) {
    ByteBuffer touchByteBuffer = ByteBuffer.allocate(18);
    // 触摸事件
    touchByteBuffer.put((byte) 0);
    // 触摸类型
    touchByteBuffer.putInt(action);
    // pointerId
    touchByteBuffer.putInt(p);
    // 坐标位置
    touchByteBuffer.putFloat((float) x / floatVideoWidth);
    touchByteBuffer.putFloat((float) y / floatVideoHeight);
    if (client.videoDecode.videoSize.getFirst() > client.videoDecode.videoSize.getSecond())
      touchByteBuffer.put((byte) 1);
    else touchByteBuffer.put((byte) 0);
    touchByteBuffer.flip();
    writeControlStream(touchByteBuffer.array());
  }

  // 发送按键事件
  public void sendKeyEvent(int key) {
    ByteBuffer navByteBuffer = ByteBuffer.allocate(5);
    // 输入事件
    navByteBuffer.put((byte) 1);
    // 按键类型
    navByteBuffer.putInt(key);
    navByteBuffer.flip();
    writeControlStream(navByteBuffer.array());
  }

  // 发送剪切板事件
  private void sendClipboardEvent() {
    byte[] tmpTextByte = nowClipboardText.getBytes(StandardCharsets.UTF_8);
    if (tmpTextByte.length > 5000) return;
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 2);
    byteBuffer.putInt(tmpTextByte.length);
    byteBuffer.flip();
    writeControlStream(byteBuffer.array());
    writeControlStream(tmpTextByte);
  }

  // 发送屏幕控制事件
  private void sendScreenModeEvent(int mode) {
    byte[] bytes = new byte[]{3, (byte) mode};
    writeControlStream(bytes);
  }

  private void writeControlStream(byte[] bytes) {
    controlOutBuffer.write(bytes);
    synchronized (controlOutBuffer) {
      controlOutBuffer.notify();
    }
  }
}
