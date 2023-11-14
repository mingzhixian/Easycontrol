package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import top.saymzx.easycontrol.app.client.Controller;
import top.saymzx.easycontrol.app.databinding.ModuleSmallViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class SmallView {
  private final ClientView clientView;

  private boolean isShow = false;

  // 悬浮窗
  private final ModuleSmallViewBinding smallView = ModuleSmallViewBinding.inflate(AppData.main.getLayoutInflater());
  private final WindowManager.LayoutParams smallViewParams =
    new WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      200,
      200,
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
      LayoutParamsFlagFocus,
      PixelFormat.TRANSLUCENT
    );

  private static final int baseLayoutParamsFlag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
  private static final int LayoutParamsFlagFocus = baseLayoutParamsFlag | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
  private static final int LayoutParamsFlagNoFocus = baseLayoutParamsFlag | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

  public SmallView(ClientView clientView) {
    this.clientView = clientView;
    smallViewParams.gravity = Gravity.START | Gravity.TOP;
    // 设置监听控制
    setFloatVideoListener();
    setButtonListener(null);
    setReSizeListener();
    setBarListener();
  }

  public void show(Controller controller) {
    if (!isShow) {
      isShow = true;
      // 隐藏工具栏
      smallView.barView.setVisibility(View.GONE);
      // 设置监听
      setButtonListener(controller);
      // 显示
      AppData.windowManager.addView(smallView.getRoot(), smallViewParams);
      showSmallViewAnim();
      // 更新TextureView
      smallView.textureViewLayout.addView(clientView.textureView, 0);
      calculateSite(PublicTools.getScreenSize());
    }
  }

  public void hide() {
    if (isShow) {
      isShow = false;
      smallView.textureViewLayout.removeView(clientView.textureView);
      AppData.windowManager.removeView(smallView.getRoot());
    }
  }

  // 计算合适位置
  public void calculateSite(Pair<Integer, Integer> screenSize) {
    clientView.updateTextureViewSize(new Pair<>(screenSize.first * 3 / 4, screenSize.second * 3 / 4));
    setCenter(screenSize);
  }

  // 更改Small View的形态
  public void showSmallViewAnim() {
    // 创建平移动画
    smallView.getRoot().setTranslationY(400);
    float endY = 0;
    // 创建透明度动画
    smallView.getRoot().setAlpha(0f);
    float endAlpha = 1f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = smallView.getRoot().animate()
      .translationY(endY)
      .alpha(endAlpha)
      .setDuration(400)
      .setInterpolator(new OvershootInterpolator());

    // 启动动画
    animator.start();
  }

  // 设置焦点监听
  private boolean viewFocus = true;

  @SuppressLint("ClickableViewAccessibility")
  private void setFloatVideoListener() {
    smallView.getRoot().setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_OUTSIDE) setViewFocus(false);
      return false;
    });
  }

  public void setViewFocus(boolean toFocus) {
    if (!toFocus && viewFocus) {
      smallViewParams.flags = LayoutParamsFlagNoFocus;
      AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
      viewFocus = false;
    } else if (toFocus && !viewFocus) {
      smallViewParams.flags = LayoutParamsFlagFocus;
      AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
      viewFocus = true;
    }
  }

  // 设置上横条监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicBoolean isFilp = new AtomicBoolean(false);
    AtomicInteger xx = new AtomicInteger();
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger paramsX = new AtomicInteger();
    AtomicInteger paramsY = new AtomicInteger();
    AtomicReference<Pair<Integer, Integer>> screenSize = new AtomicReference<>(new Pair<>(0, 0));
    smallView.bar.setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          xx.set((int) event.getRawX());
          yy.set((int) event.getRawY());
          paramsX.set(smallViewParams.x);
          paramsY.set(smallViewParams.y);
          isFilp.set(false);
          screenSize.set(PublicTools.getScreenSize());
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          int x = (int) event.getRawX();
          int y = (int) event.getRawY();
          int flipX = x - xx.get();
          int flipY = y - yy.get();
          // 适配一些机器将点击视作小范围移动(小于4的圆内不做处理)
          if (!isFilp.get()) {
            if (flipX * flipX + flipY * flipY < 16) return true;
            isFilp.set(true);
          }
          // 拖动限制
          if (x < 200 | x > screenSize.get().first - 200 | y < 200 | y > screenSize.get().second - 200)
            return true;
          // 更新
          smallViewParams.x = paramsX.get() + flipX;
          smallViewParams.y = paramsY.get() + flipY;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          if (!isFilp.get())
            clientView.changeBarViewAnim(smallView.barView, true);
          break;
      }
      return true;
    });
  }

  // 居中显示
  private void setCenter(Pair<Integer, Integer> screenSize) {
    ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
    smallViewParams.x = (screenSize.first - layoutParams.width) / 2;
    smallViewParams.y = (screenSize.second - layoutParams.height) / 2;
    AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
  }

  // 设置按钮监听
  private void setButtonListener(Controller controller) {
    if (controller != null) {
      smallView.buttonBack.setOnClickListener(v -> controller.sendKeyEvent(4));
      smallView.buttonHome.setOnClickListener(v -> controller.sendKeyEvent(3));
      smallView.buttonSwitch.setOnClickListener(v -> controller.sendKeyEvent(187));
      smallView.buttonPower.setOnClickListener(v -> controller.sendPowerEvent());
    }
    smallView.buttonMini.setOnClickListener(v -> clientView.changeToMini());
    smallView.buttonFull.setOnClickListener(v -> clientView.changeToFull());
    smallView.buttonClose.setOnClickListener(v -> clientView.hide(true));
  }

  // 设置悬浮窗大小拖动按钮监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setReSizeListener() {
    int minSize = PublicTools.dp2px(150f);
    smallView.reSize.setOnTouchListener((v, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        // 更新位置大小
        int sizeX = (int) (event.getRawX() - smallViewParams.x);
        int sizeY = (int) (event.getRawY() - smallViewParams.y);
        if (sizeX < minSize || sizeY < minSize) return true;
        clientView.updateTextureViewSize(new Pair<>(sizeX, sizeY));
      }
      return true;
    });
  }

}

