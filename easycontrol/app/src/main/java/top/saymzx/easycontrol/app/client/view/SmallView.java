package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Controller;
import top.saymzx.easycontrol.app.databinding.ModuleSmallViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class SmallView extends ViewOutlineProvider {
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
    setReSizeListener();
    setBarListener();
    // 设置圆角
    smallView.getRoot().setOutlineProvider(this);
    smallView.getRoot().setClipToOutline(true);
  }

  public void show(Controller controller) {
    if (!isShow) {
      isShow = true;
      // 初始化
      smallView.barView.setVisibility(View.GONE);
      ViewGroup.LayoutParams layoutParams = smallView.textureViewLayout.getLayoutParams();
      layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
      layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
      smallView.textureViewLayout.setLayoutParams(layoutParams);
      // 设置监听
      setButtonListener(controller);
      // 显示
      clientView.viewAnim(smallView.getRoot(), true, 0, PublicTools.dp2px(40f), (isStart -> {
        if (isStart) {
          smallView.getRoot().setVisibility(View.VISIBLE);
          AppData.windowManager.addView(smallView.getRoot(), smallViewParams);
        }
      }));
      // 更新TextureView
      smallView.textureViewLayout.addView(clientView.textureView, 0);
      Pair<Integer, Integer> screenSize = PublicTools.getScreenSize();
      clientView.updateMaxSize(new Pair<>(screenSize.first * 4 / 5, screenSize.second * 4 / 5));
      calculateSite(screenSize);
    }
  }

  public void hide() {
    if (isShow) {
      isShow = false;
      ViewGroup.LayoutParams layoutParams = smallView.textureViewLayout.getLayoutParams();
      layoutParams.width = smallView.textureViewLayout.getMeasuredWidth();
      layoutParams.height = smallView.textureViewLayout.getMeasuredHeight();
      smallView.textureViewLayout.setLayoutParams(layoutParams);
      smallView.textureViewLayout.removeView(clientView.textureView);
      clientView.viewAnim(smallView.getRoot(), false, 0, PublicTools.dp2px(40f), (isStart -> {
        if (!isStart) {
          smallView.getRoot().setVisibility(View.GONE);
          AppData.windowManager.removeView(smallView.getRoot());
        }
      }));
    }
  }

  // 获取默认宽高比，用于修改分辨率使用
  public static float getResolution() {
    return 0.55f;
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
            smallView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(R.color.clientBarSecond)));
          }
          // 拖动限制
          if (x < 100 | x > screenSize.get().first - 100 | y < 150 | y > screenSize.get().second - 100) return true;
          // 更新
          smallViewParams.x = paramsX.get() + flipX;
          smallViewParams.y = paramsY.get() + flipY;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          if (!isFilp.get()) changeBarView();
          smallView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(R.color.translucent)));
          break;
      }
      return true;
    });
  }

  // 居中显示
  public void calculateSite(Pair<Integer, Integer> screenSize) {
    ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
    smallViewParams.x = (screenSize.first - layoutParams.width) / 2;
    smallViewParams.y = (screenSize.second - layoutParams.height) / 2;
    AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
  }

  // 设置按钮监听
  private void setButtonListener(Controller controller) {
    smallView.buttonBack.setOnClickListener(v -> controller.sendKeyEvent(4));
    smallView.buttonHome.setOnClickListener(v -> controller.sendKeyEvent(3));
    smallView.buttonSwitch.setOnClickListener(v -> controller.sendKeyEvent(187));
    smallView.buttonPower.setOnClickListener(v -> controller.sendPowerEvent());
    smallView.buttonMini.setOnClickListener(v -> clientView.changeToMini());
    smallView.buttonMiniCircle.setOnClickListener(v -> clientView.changeToMini());
    smallView.buttonFull.setOnClickListener(v -> clientView.changeToFull());
    smallView.buttonClose.setOnClickListener(v -> clientView.hide(true));
    smallView.buttonCloseCircle.setOnClickListener(v -> clientView.hide(true));
    smallView.buttonNavBar.setOnClickListener(v -> setNavBarHide());
  }

  // 导航栏隐藏
  private void setNavBarHide() {
    changeBarView();
    boolean isShow = smallView.navBar.getVisibility() == View.GONE;
    smallView.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    smallView.buttonNavBar.setImageResource(isShow ? R.drawable.hide_nav : R.drawable.show_nav);
  }

  private void changeBarView() {
    boolean toShowView = smallView.barView.getVisibility() == View.GONE;
    clientView.viewAnim(smallView.barView, toShowView, 0, PublicTools.dp2px(-40f), (isStart -> {
      if (isStart && toShowView) smallView.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) smallView.barView.setVisibility(View.GONE);
    }));
  }

  // 设置悬浮窗大小拖动按钮监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setReSizeListener() {
    int minSize = PublicTools.dp2px(150f);
    smallView.reSize.setOnTouchListener((v, event) -> {
      int sizeX = (int) (event.getRawX() - smallViewParams.x);
      int sizeY = (int) (event.getRawY() - smallViewParams.y);
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        if (sizeX < minSize || sizeY < minSize) return true;
        clientView.updateMaxSize(new Pair<>(sizeX, sizeY));
      }
      return true;
    });
  }

  // 更新模糊背景图
  public void updateBackImage(Drawable drawable) {
    smallView.getRoot().setBackground(drawable);
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

}

