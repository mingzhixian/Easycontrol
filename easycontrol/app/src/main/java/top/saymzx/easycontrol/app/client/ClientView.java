package top.saymzx.easycontrol.app.client;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import top.saymzx.easycontrol.app.FullActivity;
import top.saymzx.easycontrol.app.entity.AppData;

public class ClientView implements TextureView.SurfaceTextureListener {
  private final Client client;

  // UI模式，0为未显示，1为全屏，2为小窗，3为mini侧边条
  private int uiMode = 0;
  private final SmallView smallView;
  private final MiniView miniView;

  private SurfaceTexture mSurfaceTexture;

  Pair<Integer, Integer> videoSize;
  private Pair<Integer, Integer> surfaceSize;

  public ClientView(Client client) {
    this.client = client;
    smallView = new SmallView(client::handleEvent);
    miniView = new MiniView(client::handleEvent);
  }

  public void changeToFull() {
    if (FullActivity.isShow) {
      Toast.makeText(AppData.main, "有设备正在全屏控制", Toast.LENGTH_SHORT).show();
    } else {
      hide();
      uiMode = 1;
      Pair<Integer, Integer> screenSize = AppData.publicTools.getScreenSize();
      FullActivity.show(client::handleEvent, client.controller, isPortrait() == screenSize.second > screenSize.first);
    }
  }

  public void changeToSmall() {
    hide();
    uiMode = 2;
    smallView.show(client.controller);
  }

  public void changeToMini() {
    hide();
    uiMode = 3;
    miniView.show();
  }

  public void hide() {
    client.isNormalPlay = false;
    if (uiMode == 1) FullActivity.hide();
    else if (uiMode == 2) smallView.hide();
    else if (uiMode == 3) miniView.hide();
  }

  // 旋转
  public void changeRotation() {
    videoSize = new Pair<>(videoSize.second, videoSize.first);
    if (uiMode == 1) {
      client.isNormalPlay = false;
      FullActivity.changeRotation();
    } else if (uiMode == 2) smallView.changeRotation();
  }

  // 检查方向
  private boolean isPortrait() {
    return videoSize.second > videoSize.first;
  }

  // 更新Surface
  public void updateSurface() {
    SurfaceView surfaceView = null;
    if (uiMode == 1) surfaceView = FullActivity.surfaceView;
    else if (uiMode == 2) surfaceView = smallView.surfaceView;
    if (surfaceView != null) {
      client.videoDecode.updateSurface(surfaceView.getHolder().getSurface());
      updateSurfaceSize();
      // 设置触摸监听
      setSurfaceListener(surfaceView);
      client.isNormalPlay = true;
    }
  }

  public void updateSurfaceSize() {
    Pair<Integer, Integer> maxSize = null;
    if (uiMode == 1) maxSize = FullActivity.maxSize;
    else if (uiMode == 2) maxSize = smallView.maxSize;
    if (maxSize != null) {
      // 根据原画面大小videoSize计算在maxSize空间内的最大缩放大小
      int tmp1 = videoSize.second * maxSize.first / videoSize.first;
      // 横向最大不会超出
      if (maxSize.second > tmp1) surfaceSize = new Pair<>(maxSize.first, tmp1);
        // 竖向最大不会超出
      else
        surfaceSize = new Pair<>(videoSize.first * maxSize.second / videoSize.second, maxSize.second);
      // 更新大小
      SurfaceView surfaceView = null;
      if (uiMode == 1) surfaceView = FullActivity.surfaceView;
      else if (uiMode == 2) surfaceView = smallView.surfaceView;
      if (surfaceView != null) {
        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
        layoutParams.width = surfaceSize.first;
        layoutParams.height = surfaceSize.second;
        surfaceView.setLayoutParams(layoutParams);
      }
    }
  }

  // 设置视频区域触摸监听
  public int touchTime = 30;

  @SuppressLint("ClickableViewAccessibility")
  private void setSurfaceListener(SurfaceView surfaceView) {
    // 视频触摸控制
    int[] pointerList = new int[20];
    surfaceView.setOnTouchListener((v, event) -> {
      // 开始多倍发包，减少阻塞
      touchTime = 0;
      // 处理触摸事件
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        int i = event.getActionIndex();
        int x = (int) event.getX(i);
        int y = (int) event.getY(i);
        int p = event.getPointerId(i);
        // 记录xy信息
        pointerList[p] = x;
        pointerList[10 + p] = y;
        client.controller.sendTouchEvent(MotionEvent.ACTION_DOWN, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
        int i = event.getActionIndex();
        int x = (int) event.getX(i);
        int y = (int) event.getY(i);
        int p = event.getPointerId(i);
        client.controller.sendTouchEvent(MotionEvent.ACTION_UP, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second);
      } else {
        for (int i = 0; i < event.getPointerCount(); i++) {
          int x = (int) event.getX(i);
          int y = (int) event.getY(i);
          int p = event.getPointerId(i);
          // 适配一些机器将点击视作小范围移动(小于3的圆内不做处理)
          if (pointerList[p] != -1) {
            if ((pointerList[p] - x) * (pointerList[p] - x) + (pointerList[10 + p] - y) * (pointerList[10 + p] - y) < 9)
              return true;
            pointerList[p] = -1;
          }
          client.controller.sendTouchEvent(MotionEvent.ACTION_MOVE, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second);
        }
      }
      return true;
    });
  }

  @Override
  public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
    TextureView textureView = null;
    if (uiMode == 1) textureView = FullActivity.context.fullActivity.textureView;
    else if (uiMode == 2) textureView = smallView.smallView.textureView;
    if (textureView != null) {
      textureView.setSurfaceTexture(mSurfaceTexture);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

  }

  @Override
  public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
    mSurfaceTexture = surfaceTexture;
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

  }
}
