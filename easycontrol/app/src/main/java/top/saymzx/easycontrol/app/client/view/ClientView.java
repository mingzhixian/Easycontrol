package top.saymzx.easycontrol.app.client.view;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Objects;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientView extends ViewOutlineProvider implements TextureView.SurfaceTextureListener {
  private final Client client;
  private final boolean setResolution;
  public final TextureView textureView = new TextureView(AppData.main);
  private SurfaceTexture surfaceTexture;

  // UI模式，0为未显示，1为全屏，2为小窗，3为mini侧边条
  private int uiMode = 0;
  private final SmallView smallView = new SmallView(this);
  private final MiniView miniView = new MiniView(this);

  private Pair<Integer, Integer> layoutSize;
  public Pair<Integer, Integer> videoSize;
  private Pair<Integer, Integer> surfaceSize;

  public ClientView(Client client, boolean setResolution) {
    this.client = client;
    this.setResolution = setResolution;
    setTouchListener();
    textureView.setSurfaceTextureListener(this);
    textureView.setOutlineProvider(this);
    textureView.setClipToOutline(true);
  }

  public void changeToFull() {
    if (FullActivity.isShow) {
      Toast.makeText(AppData.main, "有设备正在全屏控制", Toast.LENGTH_SHORT).show();
    } else {
      client.isNormalPlay = true;
      hide(false);
      uiMode = 1;
      FullActivity.show(this, client.controller);
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
    uiMode = 0;
    if (isRelease) {
      if (surfaceTexture != null) surfaceTexture.release();
      client.release();
    }
  }

  // 处理外部旋转
  public void hasChangeRotation(Pair<Integer, Integer> screenSize) {
    if (uiMode == 2) smallView.calculateSite(screenSize);
    else if (uiMode == 3) miniView.calculateSite(screenSize);
  }

  // 画面容器大小改变
  public void changeLayoutSize(Pair<Integer, Integer> layoutSize) {
    if (this.layoutSize != null && Objects.equals(this.layoutSize.first, layoutSize.first) && Objects.equals(this.layoutSize.second, layoutSize.second)) return;
    this.layoutSize = layoutSize;
    if (setResolution) {
      client.controller.sendChangeSizeEvent(layoutSize);
      if (surfaceSize == null) reCalculateTextureViewSize();
    } else reCalculateTextureViewSize();
  }

  // 重新计算TextureView大小
  private final int minSize = PublicTools.dp2px(150f);

  public void reCalculateTextureViewSize() {
    // 根据原画面大小videoSize计算在layoutSize空间内的最大缩放大小
    int tmp1 = videoSize.second * layoutSize.first / videoSize.first;
    // 横向最大不会超出
    if (layoutSize.second > tmp1) surfaceSize = new Pair<>(layoutSize.first, tmp1);
      // 竖向最大不会超出
    else surfaceSize = new Pair<>(videoSize.first * layoutSize.second / videoSize.second, layoutSize.second);
    // 避免画面过小
    if (surfaceSize.first < minSize) surfaceSize = new Pair<>(minSize, surfaceSize.second * minSize / surfaceSize.first);
    else if (surfaceSize.second < minSize) surfaceSize = new Pair<>(surfaceSize.first * minSize / surfaceSize.second, minSize);
    // 更新大小
    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
    layoutParams.width = surfaceSize.first;
    layoutParams.height = surfaceSize.second;
    textureView.setLayoutParams(layoutParams);
  }

  // 设置视频区域触摸监听
  @SuppressLint("ClickableViewAccessibility")
  private void setTouchListener() {
    int[] pointerList = new int[20];
    textureView.setOnTouchListener((view, event) -> {
      int offsetTime = (int) (event.getEventTime() - event.getDownTime());
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
        client.controller.sendTouchEvent(MotionEvent.ACTION_DOWN, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second, offsetTime);
        // 如果是小窗模式则需获取焦点，以获取剪切板同步
        if (uiMode == 2) smallView.setViewFocus(true);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
        int i = event.getActionIndex();
        int x = (int) event.getX(i);
        int y = (int) event.getY(i);
        int p = event.getPointerId(i);
        client.controller.sendTouchEvent(MotionEvent.ACTION_UP, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second, offsetTime);
      } else {
        for (int i = 0; i < event.getPointerCount(); i++) {
          int x = (int) event.getX(i);
          int y = (int) event.getY(i);
          int p = event.getPointerId(i);
          // 适配一些机器将点击视作小范围移动(小于3的圆内不做处理)
          if (pointerList[p] != -1) {
            if ((pointerList[p] - x) * (pointerList[p] - x) + (pointerList[10 + p] - y) * (pointerList[10 + p] - y) < 9) return true;
            pointerList[p] = -1;
          }
          client.controller.sendTouchEvent(MotionEvent.ACTION_MOVE, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second, offsetTime);
        }
      }
      return true;
    });
  }

  // 更改Bar View的形态
  public void changeBarViewAnim(View view, boolean toDown) {
    int translationY = PublicTools.dp2px(40f) * (toDown ? -1 : 1);
    boolean toShowBarView = view.getVisibility() == View.GONE;
    // 创建平移动画
    view.setTranslationY(toShowBarView ? translationY : 0);
    float endY = toShowBarView ? 0 : translationY;
    // 创建透明度动画
    view.setAlpha(toShowBarView ? 0f : 1f);
    float endAlpha = toShowBarView ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = view.animate()
      .translationY(endY)
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
  public void getOutline(View view, Outline outline) {
    Rect rect = new Rect();
    view.getGlobalVisibleRect(rect);
    int leftMargin = 0;
    int topMargin = 0;
    Rect selfRect = new Rect(leftMargin, topMargin, rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
    outline.setRoundRect(selfRect, AppData.main.getResources().getDimension(R.dimen.round));
  }

  @Override
  public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
    // 初始化
    if (this.surfaceTexture == null) {
      this.surfaceTexture = surfaceTexture;
      client.videoDecode.setSurface(new Surface(this.surfaceTexture));
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
