package top.saymzx.easycontrol.app.client;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import java.util.concurrent.atomic.AtomicInteger;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ModuleMiniViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;

public class MiniView {

  private final Client.MyFunctionInt myFunctionInt;

  private boolean isShow = false;

  // 迷你悬浮窗
  ModuleMiniViewBinding miniView = ModuleMiniViewBinding.inflate(AppData.main.getLayoutInflater());
  private final WindowManager.LayoutParams miniViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
    PixelFormat.TRANSLUCENT
  );

  private static int color = 0;

  public MiniView(Client.MyFunctionInt myFunctionInt) {
    this.myFunctionInt = myFunctionInt;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    // Bar颜色
    int barColor = R.color.bar1;
    switch (color++ % 4) {
      case 1:
        barColor = R.color.bar2;
        break;
      case 2:
        barColor = R.color.bar3;
        break;
      case 3:
        barColor = R.color.bar4;
        break;
    }
    miniView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(barColor)));
    // 位置
    miniViewParams.x = -1 * AppData.publicTools.dp2px(10f);
    miniViewParams.y = 200;
  }

  public void show() {
    if (!isShow) {
      isShow = true;
      // 设置监听控制
      setBarListener();
      // 显示
      showViewAnim();
    }
  }

  public void hide() {
    if (isShow) {
      isShow = false;
      showViewAnim();
    }
  }

  // 显示隐藏动画
  private void showViewAnim() {
    int width = AppData.publicTools.dp2px(-30f);
    // 创建平移动画
    miniView.getRoot().setTranslationX(isShow ? width : 0);
    float endX = isShow ? 0 : width;
    // 创建透明度动画
    miniView.getRoot().setAlpha(isShow ? 0f : 1f);
    float endAlpha = isShow ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = miniView.getRoot().animate()
      .translationX(endX)
      .alpha(endAlpha)
      .setDuration(300)
      .setInterpolator(new LinearInterpolator());

    // 显示或隐藏
    animator.setListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {
        if (isShow) AppData.main.getWindowManager().addView(miniView.getRoot(), miniViewParams);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        if (!isShow) AppData.main.getWindowManager().removeView(miniView.getRoot());
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

  // 设置监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger paramsY = new AtomicInteger();
    miniView.getRoot().setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          yy.set((int) event.getRawY());
          paramsY.set(miniViewParams.y);
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          miniViewParams.y = paramsY.get() + (int) event.getRawY() - yy.get();
          AppData.main.getWindowManager().updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          int flipY = (int) (yy.get() - event.getRawY());
          if (flipY * flipY < 16)
            myFunctionInt.handleEvent(Client.Event_CHANGE_SMALL);
      }
      return true;
    });
  }

}
