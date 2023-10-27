package top.saymzx.easycontrol.app.client;

import android.annotation.SuppressLint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import top.saymzx.easycontrol.app.databinding.ModuleSmallViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;

public class SmallView {
  private final Client.MyFunctionInt myFunctionInt;

  public Pair<Integer, Integer> maxSize;

  private boolean isShow = false;

  // 悬浮窗
  public final ModuleSmallViewBinding smallView = ModuleSmallViewBinding.inflate(AppData.main.getLayoutInflater());
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

  private static final int baseLayoutParamsFlag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
  private static final int LayoutParamsFlagFocus = baseLayoutParamsFlag | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
  private static final int LayoutParamsFlagNoFocus = baseLayoutParamsFlag | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

  public SmallView(Client.MyFunctionInt myFunctionInt) {
    this.myFunctionInt = myFunctionInt;
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
      // 显示
      AppData.main.getWindowManager().addView(smallView.getRoot(), smallViewParams);
      // 隐藏工具栏
      smallView.barView.setVisibility(View.GONE);
      // 设置监听
      setButtonListener(controller);
      // 更新MaxSize
      Pair<Integer, Integer> screenSize = AppData.publicTools.getScreenSize();
      maxSize = new Pair<>(screenSize.first * 3 / 4, screenSize.second * 3 / 4);
      // 更新Surface
      listenerSurfaceOk();
    }
  }

  public void hide() {
    if (isShow) {
      isShow = false;
      AppData.main.getWindowManager().removeView(smallView.getRoot());
    }
  }

  public void changeRotation() {
    Pair<Integer, Integer> screenSize = AppData.publicTools.getScreenSize();
    maxSize = new Pair<>(screenSize.first * 3 / 4, screenSize.second * 3 / 4);
    myFunctionInt.handleEvent(Client.Event_UPDATE_SURFACE_SIZE);
    // 居中显示
    setCenter();
  }

  // 设置焦点监听
  @SuppressLint("ClickableViewAccessibility")
  private void setFloatVideoListener() {
    final boolean[] viewFocus = {true};
    smallView.getRoot().setOnTouchListener((v, event) -> {
      if (viewFocus[0] && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
        smallViewParams.flags = LayoutParamsFlagNoFocus;
        AppData.main.getWindowManager().updateViewLayout(smallView.getRoot(), smallViewParams);
        viewFocus[0] = false;
      } else if (!viewFocus[0]) {
        smallViewParams.flags = LayoutParamsFlagFocus;
        AppData.main.getWindowManager().updateViewLayout(smallView.getRoot(), smallViewParams);
        viewFocus[0] = true;
      }
      return false;
    });
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
          screenSize.set(AppData.publicTools.getScreenSize());
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
          if (x < 100 | x > screenSize.get().first - 100 | y < 100 | y > screenSize.get().second - 100)
            return true;
          // 更新
          smallViewParams.x = paramsX.get() + flipX;
          smallViewParams.y = paramsY.get() + flipY;
          AppData.main.getWindowManager().updateViewLayout(smallView.getRoot(), smallViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          if (!isFilp.get())
            AppData.publicTools.changeBarViewAnim(smallView.barView, AppData.publicTools.dp2px(-40f));
          break;
      }
      return true;
    });
  }

  // 等待Surface准备好后更新输出Surface
  private void listenerSurfaceOk() {
    surfaceView = smallView.surfaceView;
    surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        myFunctionInt.handleEvent(Client.Event_UPDATE_SURFACE);
        setCenter();
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {

      }
    });
  }

  // 居中显示
  private void setCenter() {
    Pair<Integer, Integer> screenSize = AppData.publicTools.getScreenSize();
    // 更新
    ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
    smallViewParams.x = (screenSize.first - layoutParams.width) / 2;
    smallViewParams.y = (screenSize.second - layoutParams.height) / 2;
    AppData.main.getWindowManager().updateViewLayout(smallView.getRoot(), smallViewParams);
  }

  // 设置按钮监听
  private void setButtonListener(Controller controller) {
    if (controller != null) {
      smallView.buttonBack.setOnClickListener(v -> controller.sendKeyEvent(4));
      smallView.buttonHome.setOnClickListener(v -> controller.sendKeyEvent(3));
      smallView.buttonSwitch.setOnClickListener(v -> controller.sendKeyEvent(187));
    }
    smallView.buttonMini.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CHANGE_MINI));
    smallView.buttonFull.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CHANGE_FULL));
    smallView.buttonClose.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CLOSE));
  }

  // 设置悬浮窗大小拖动按钮监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setReSizeListener() {
    int minSize = AppData.publicTools.dp2px(100f);
    smallView.reSize.setOnTouchListener((v, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        // 更新位置大小
        int sizeX = (int) (event.getRawX() - smallViewParams.x);
        int sizeY = (int) (event.getRawY() - smallViewParams.y);
        if (sizeX < minSize || sizeY < minSize) return true;
        maxSize = new Pair<>(sizeX, sizeY);
        myFunctionInt.handleEvent(Client.Event_UPDATE_SURFACE_SIZE);
      }
      return true;
    });
  }

}

