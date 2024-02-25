package top.saymzx.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.view.FullActivity;
import top.saymzx.easycontrol.app.client.view.MiniView;
import top.saymzx.easycontrol.app.client.view.SmallView;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.entity.MyInterface;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientController implements TextureView.SurfaceTextureListener {
  private static final HashMap<String, ClientController> allController = new HashMap<>();

  private boolean isClose = false;
  private final Device device;
  private final ClientStream clientStream;
  private final MyInterface.MyFunctionBoolean handle;
  public final TextureView textureView = new TextureView(AppData.applicationContext);
  private SurfaceTexture surfaceTexture;

  private final SmallView smallView;
  private final MiniView miniView;
  private FullActivity fullView;

  private Pair<Integer, Integer> videoSize;
  private Pair<Integer, Integer> maxSize;
  private Pair<Integer, Integer> surfaceSize;

  private final HandlerThread handlerThread = new HandlerThread("easycontrol_controler");
  private final Handler handler;

  public ClientController(Device device, ClientStream clientStream, MyInterface.MyFunctionBoolean handle) {
    allController.put(device.uuid, this);
    this.device = device;
    this.clientStream = clientStream;
    this.handle = handle;
    smallView = new SmallView(device);
    miniView = new MiniView(device);
    setTouchListener();
    textureView.setSurfaceTextureListener(this);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    // 启动界面
    handleControll(device.uuid, device.changeToFullOnConnect ? "changeToFull" : "changeToSmall", null);
    // 运行启动时操作
    if (device.customResolutionOnConnect) handleControll(device.uuid, "writeByteBuffer", ControlPacket.createChangeResolutionEvent(device.customResolutionWidth, device.customResolutionHeight));
    if (device.wakeOnConnect) handleControll(device.uuid, "buttonWake", null);
    if (device.lightOffOnConnect) handler.postDelayed(() -> handleControll(device.uuid, "buttonLightOff", null), 2000);
  }

  public static void handleControll(String uuid, String action, ByteBuffer byteBuffer) {
    ClientController clientController = allController.get(uuid);
    if (clientController == null) return;
    clientController.handler.post(() -> handleAction(clientController, action, byteBuffer));
  }

  private static void handleAction(ClientController clientController, String action, ByteBuffer byteBuffer) {
    try {
      if (action.equals("changeToSmall")) clientController.changeToSmall();
      else if (action.equals("changeToFull")) clientController.changeToFull();
      else if (action.equals("changeToMini")) clientController.changeToMini(byteBuffer);
      else if (action.equals("close")) clientController.close(null);
      else if (action.equals("buttonPower")) clientController.clientStream.writeToMain(ControlPacket.createPowerEvent(-1));
      else if (action.equals("buttonWake")) clientController.clientStream.writeToMain(ControlPacket.createPowerEvent(1));
      else if (action.equals("buttonLock")) clientController.clientStream.writeToMain(ControlPacket.createPowerEvent(0));
      else if (action.equals("buttonLight")) clientController.clientStream.writeToMain(ControlPacket.createLightEvent(Display.STATE_ON));
      else if (action.equals("buttonLightOff")) clientController.clientStream.writeToMain(ControlPacket.createLightEvent(Display.STATE_UNKNOWN));
      else if (action.equals("buttonBack")) clientController.clientStream.writeToMain(ControlPacket.createKeyEvent(4, 0));
      else if (action.equals("buttonHome")) clientController.clientStream.writeToMain(ControlPacket.createKeyEvent(3, 0));
      else if (action.equals("buttonSwitch")) clientController.clientStream.writeToMain(ControlPacket.createKeyEvent(187, 0));
      else if (action.equals("buttonRotate")) clientController.clientStream.writeToMain(ControlPacket.createRotateEvent());
      else if (action.equals("keepAlive")) clientController.clientStream.writeToMain(ControlPacket.createKeepAlive());
      else if (action.equals("checkSizeAndSite")) clientController.checkSizeAndSite();
      else if (action.equals("checkClipBoard")) clientController.checkClipBoard();
      else if (byteBuffer == null) return;
      else if (action.equals("writeByteBuffer")) clientController.clientStream.writeToMain(byteBuffer);
      else if (action.equals("updateMaxSize")) clientController.updateMaxSize(byteBuffer);
      else if (action.equals("updateVideoSize")) clientController.updateVideoSize(byteBuffer);
      else if (action.equals("runShell")) clientController.runShell(byteBuffer);
      else if (action.equals("setClipBoard")) clientController.setClipBoard(byteBuffer);
    } catch (Exception ignored) {
      clientController.close(AppData.applicationContext.getString(R.string.toast_stream_closed));
    }
  }

  public static void setFullView(String uuid, FullActivity fullView) {
    ClientController clientController = allController.get(uuid);
    if (clientController == null) return;
    clientController.fullView = fullView;
  }

  public static Device getDevice(String uuid) {
    ClientController clientController = allController.get(uuid);
    if (clientController == null) return null;
    return clientController.device;
  }

  public Pair<Integer, Integer> getVideoSize() {
    return videoSize;
  }

  public Surface getSurface() {
    return new Surface(surfaceTexture);
  }

  public static TextureView getTextureView(String uuid) {
    ClientController clientController = allController.get(uuid);
    if (clientController == null) return null;
    return clientController.textureView;
  }

  private synchronized void changeToFull() {
    hide();
    Intent intent = new Intent(AppData.mainActivity, FullActivity.class);
    intent.putExtra("uuid", device.uuid);
    AppData.mainActivity.startActivity(intent);
  }

  private synchronized void changeToSmall() {
    hide();
    AppData.uiHandler.post(smallView::show);
  }

  private synchronized void changeToMini(ByteBuffer byteBuffer) {
    hide();
    AppData.uiHandler.post(() -> miniView.show(byteBuffer));
  }

  private synchronized void hide() {
    if (fullView != null) AppData.uiHandler.post(fullView::hide);
    fullView = null;
    AppData.uiHandler.post(smallView::hide);
    AppData.uiHandler.post(miniView::hide);
  }

  private void close(String error) {
    if (isClose) return;
    isClose = true;
    hide();
    // 运行断开时操作
    if (device.lockOnClose) handleControll(device.uuid, "buttonLock", null);
      // 开启了自动锁定，就没必要发送打开背光了
    else if (device.lightOnClose) handleControll(device.uuid, "buttonLight", null);
    if (error != null && device.isNetworkDevice() && device.reconnectOnClose) new Client(device);
    // 打印日志
    if (error != null) PublicTools.logToast("controller", error, true);
    handlerThread.interrupt();
    allController.remove(device.uuid);
    if (surfaceTexture != null) surfaceTexture.release();
    handle.run(false);
  }

  private static final int minLength = PublicTools.dp2px(200f);

  private void updateMaxSize(ByteBuffer byteBuffer) {
    int width = Math.max(byteBuffer.getInt(), minLength);
    int height = Math.max(byteBuffer.getInt(), minLength);
    this.maxSize = new Pair<>(width, height);
    AppData.uiHandler.post(this::reCalculateTextureViewSize);
  }

  private void updateVideoSize(ByteBuffer byteBuffer) {
    int width = byteBuffer.getInt();
    int height = byteBuffer.getInt();
    if (width <= 100 || height <= 100) return;
    this.videoSize = new Pair<>(width, height);
    AppData.uiHandler.post(this::reCalculateTextureViewSize);
  }

  // 重新计算TextureView大小
  private void reCalculateTextureViewSize() {
    if (maxSize == null || videoSize == null) return;
    // 根据原画面大小videoSize计算在maxSize空间内的最大缩放大小
    int tmp1 = videoSize.second * maxSize.first / videoSize.first;
    // 横向最大不会超出
    if (maxSize.second > tmp1) surfaceSize = new Pair<>(maxSize.first, tmp1);
      // 竖向最大不会超出
    else surfaceSize = new Pair<>(videoSize.first * maxSize.second / videoSize.second, maxSize.second);
    // 更新大小
    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
    layoutParams.width = surfaceSize.first;
    layoutParams.height = surfaceSize.second;
    textureView.setLayoutParams(layoutParams);
  }

  // 检查画面是否超出
  private void checkSizeAndSite() {
    // 碎碎念，感谢 波瑠卡 的关爱，今天一家四口一起去医院进年货去了，每人提了一袋子(´；ω；`)
    AppData.uiHandler.post(smallView::checkSizeAndSite);
  }

  // 设置视频区域触摸监听
  @SuppressLint("ClickableViewAccessibility")
  private void setTouchListener() {
    textureView.setOnTouchListener((view, event) -> {
      if (surfaceSize == null) return true;
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        int i = event.getActionIndex();
        pointerDownTime[i] = event.getEventTime();
        createTouchPacket(event, MotionEvent.ACTION_DOWN, i);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) createTouchPacket(event, MotionEvent.ACTION_UP, event.getActionIndex());
      else for (int i = 0; i < event.getPointerCount(); i++) createTouchPacket(event, MotionEvent.ACTION_MOVE, i);
      return true;
    });
  }

  private final int[] pointerList = new int[20];
  private final long[] pointerDownTime = new long[10];

  private void createTouchPacket(MotionEvent event, int action, int i) {
    int offsetTime = (int) (event.getEventTime() - pointerDownTime[i]);
    int x = (int) event.getX(i);
    int y = (int) event.getY(i);
    int p = event.getPointerId(i);
    if (action == MotionEvent.ACTION_MOVE) {
      // 减少发送小范围移动(小于4的圆内不做处理)
      int flipY = pointerList[10 + p] - y;
      if (flipY > -4 && flipY < 4) {
        int flipX = pointerList[p] - x;
        if (flipX > -4 && flipX < 4) return;
      }
    }
    pointerList[p] = x;
    pointerList[10 + p] = y;
    handleControll(device.uuid, "writeByteBuffer", ControlPacket.createTouchEvent(action, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second, offsetTime));
  }

  // 剪切板
  private String nowClipboardText = "";

  private void checkClipBoard() {
    ClipData clipBoard = AppData.clipBoard.getPrimaryClip();
    if (clipBoard != null && clipBoard.getItemCount() > 0) {
      String newClipBoardText = String.valueOf(clipBoard.getItemAt(0).getText());
      if (!Objects.equals(nowClipboardText, newClipBoardText)) {
        nowClipboardText = newClipBoardText;
        handleControll(device.uuid, "writeByteBuffer", ControlPacket.createClipboardEvent(nowClipboardText));
      }
    }
  }

  private void setClipBoard(ByteBuffer byteBuffer) {
    nowClipboardText = new String(byteBuffer.array());
    AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, nowClipboardText));
  }

  private void runShell(ByteBuffer byteBuffer) throws Exception {
    String cmd = new String(byteBuffer.array());
    clientStream.runShell(cmd);
  }

  @Override
  public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
    // 初始化
    if (this.surfaceTexture == null) {
      this.surfaceTexture = surfaceTexture;
      handle.run(true);
    } else textureView.setSurfaceTexture(this.surfaceTexture);
  }

  @Override
  public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
  }

  @Override
  public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
  }

}
