package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicInteger;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ModuleMiniViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class MiniView {

  private final ClientView clientView;

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

  private static int color = -1;

  public MiniView(ClientView clientView) {
    this.clientView = clientView;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    // Bar颜色
    int colorNum = color++ % 4;
    int barColor = R.color.bar1;
    if (colorNum == 1) barColor = R.color.bar2;
    else if (colorNum == 2) barColor = R.color.bar3;
    else if (colorNum == 3) barColor = R.color.bar4;
    miniView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(barColor)));
    miniViewParams.x = -1 * PublicTools.dp2px(10f);
  }

  public void show() {
    if (!isShow) {
      isShow = true;
      // 设置监听控制
      setBarListener();
      // 显示
      clientView.viewAnim(miniView.getRoot(), true, PublicTools.dp2px(-40f), 0, (isStart -> {
        if (isStart) {
          miniView.getRoot().setVisibility(View.VISIBLE);
          AppData.windowManager.addView(miniView.getRoot(), miniViewParams);
          calculateSite(PublicTools.getScreenSize());
        }
      }));
    }
  }

  public void hide() {
    if (isShow) {
      isShow = false;
      clientView.viewAnim(miniView.getRoot(), false, PublicTools.dp2px(-40f), 0, (isStart -> {
        if (!isStart) {
          miniView.getRoot().setVisibility(View.GONE);
          AppData.windowManager.removeView(miniView.getRoot());
        }
      }));
    }
  }

  // 计算合适位置
  public void calculateSite(Pair<Integer, Integer> screenSize) {
    miniViewParams.y = (screenSize.second / 5) * (color % 4 + 1);
    AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
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
          AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          int flipY = (int) (yy.get() - event.getRawY());
          if (flipY * flipY < 16) clientView.changeToSmall();
      }
      return true;
    });
  }

}
