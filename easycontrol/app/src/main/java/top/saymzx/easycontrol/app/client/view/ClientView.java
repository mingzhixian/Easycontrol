package top.saymzx.easycontrol.app.client.view;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.entity.AppData;

public class ClientView implements TextureView.SurfaceTextureListener {
  private final Client client;
  public final TextureView textureView = new TextureView(AppData.main);
  private SurfaceTexture surfaceTexture;

  // UI模式，0为未显示，1为全屏，2为小窗，3为mini侧边条
  private int uiMode = 0;
  private final SmallView smallView = new SmallView(this);
  private final MiniView miniView = new MiniView(this);

  public Pair<Integer, Integer> videoSize;
  private Pair<Integer, Integer> surfaceSize;

  public ClientView(Client client) {
    this.client = client;
    setTouchListener();
    textureView.setSurfaceTextureListener(this);
  }

  public void changeToFull() {
    if (FullActivity.isShow) {
      Toast.makeText(AppData.main, "有设备正在全屏控制", Toast.LENGTH_SHORT).show();
    } else {
      client.isNormalPlay = true;
      hide(false);
      uiMode = 1;
      FullActivity.show(this, client.controller, videoSize.second > videoSize.first);
    }
  }

  public void changeToSmall() {
    client.isNormalPlay = true;
    hide(false);
    uiMode = 2;
    smallView.show(client.controller);
  }

  public void changeToMini() {
    client.isNormalPlay = false;
    hide(false);
    uiMode = 3;
    miniView.show();
  }

  public void hide(boolean isRelease) {
    if (uiMode == 1) FullActivity.hide();
    else if (uiMode == 2) smallView.hide();
    else if (uiMode == 3) miniView.hide();
    if (isRelease) {
      if (surfaceTexture != null) surfaceTexture.release();
      client.release();
    }
  }

  // 旋转
  public void changeRotation() {
    videoSize = new Pair<>(videoSize.second, videoSize.first);
    if (uiMode == 1) FullActivity.changeRotation();
    else if (uiMode == 2) smallView.changeRotation();
  }

  // 更新Surface大小
  public void updateTextureViewSize(Pair<Integer, Integer> maxSize) {
    // 根据原画面大小videoSize计算在maxSize空间内的最大缩放大小
    int tmp1 = videoSize.second * maxSize.first / videoSize.first;
    // 横向最大不会超出
    if (maxSize.second > tmp1) surfaceSize = new Pair<>(maxSize.first, tmp1);
      // 竖向最大不会超出
    else
      surfaceSize = new Pair<>(videoSize.first * maxSize.second / videoSize.second, maxSize.second);
    // 更新大小
    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
    layoutParams.width = surfaceSize.first;
    layoutParams.height = surfaceSize.second;
    textureView.setLayoutParams(layoutParams);
  }

  // 设置视频区域触摸监听
  @SuppressLint("ClickableViewAccessibility")
  private void setTouchListener() {
    // 视频触摸控制
    int[] pointerList = new int[20];
    textureView.setOnTouchListener((view, event) -> {
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
        // 如果是小窗模式则需获取焦点，以获取剪切板同步
        if (uiMode == 2) smallView.setViewFocus(true);
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
      // 开始多倍发包，减少阻塞
      client.adb.sendMoreOk(client.stream);
      return true;
    });
  }

  // 更改Bar View的形态
  public void changeBarViewAnim(View view, int translationY) {
    boolean toShowBarView = view.getVisibility() == View.GONE;
    // 创建平移动画
    view.setTranslationY(toShowBarView ? translationY : 0);
    float endY = toShowBarView ? 0 : translationY;
    // 创建缩放动画
    view.setScaleY(toShowBarView ? 0f : 1f);
    float endScaleY = toShowBarView ? 1f : 0f;
    // 创建透明度动画
    view.setAlpha(toShowBarView ? 0f : 1f);
    float endAlpha = toShowBarView ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = view.animate()
      .translationY(endY)
      .scaleY(endScaleY)
      .alpha(endAlpha)
      .setDuration(300)
      .setInterpolator(toShowBarView ? new OvershootInterpolator() : new DecelerateInterpolator());

    // 显示或隐藏
    animator.setListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {
        if (toShowBarView) view.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        if (!toShowBarView) view.setVisibility(View.GONE);
      }

      @Override
      public void onAnimationCancel(Animator animation) {
      }

      @Override
      public void onAnimationRepeat(Animator animation) {
      }
    });

    // 启动动画
    animator.start();
  }

  @Override
  public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
    // 初始化
    if (this.surfaceTexture == null) {
      this.surfaceTexture = surfaceTexture;
      try {
        client.videoDecode.setVideoDecodec(new Surface(this.surfaceTexture));
      } catch (IOException e) {
        hide(true);
      }
      client.startSubService();
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
