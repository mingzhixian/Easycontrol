package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
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

  // 迷你悬浮窗
  private final ModuleMiniViewBinding miniView = ModuleMiniViewBinding.inflate(LayoutInflater.from(AppData.main));
  public boolean isShow = false;
  private final WindowManager.LayoutParams miniViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
    PixelFormat.TRANSLUCENT
  );

  private static int num = 0;
  private int id;
  private static final int[] site = new int[10];
  private final int height = PublicTools.dp2px(60f);

  public MiniView(ClientView clientView) {
    this.clientView = clientView;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    // Bar颜色
    int colorNum = num++ % 4;
    int barColor = R.color.bar1;
    if (colorNum == 1) barColor = R.color.bar2;
    else if (colorNum == 2) barColor = R.color.bar3;
    else if (colorNum == 3) barColor = R.color.bar4;
    miniView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(barColor)));
    miniViewParams.x = 0;
  }

  public void show() {
    if (!isShow) {
      isShow = true;
      id = num++;
      // 设置监听控制
      setBarListener();
      // 显示
      clientView.viewAnim(miniView.getRoot(), true, PublicTools.dp2px(-40f), 0, (isStart -> {
        if (isStart) {
          miniView.getRoot().setVisibility(View.VISIBLE);
          AppData.windowManager.addView(miniView.getRoot(), miniViewParams);
          calculateSite();
        }
      }));
    }
  }

  public void hide(boolean force) {
    try {
      if (force || isShow) {
        isShow = false;
        num--;
        miniView.getRoot().setVisibility(View.GONE);
        AppData.windowManager.removeView(miniView.getRoot());
      }
    } catch (Exception ignored) {
    }
  }

  // 计算合适位置
  private void calculateSite() {
    int startY;
    boolean isConflict;
    for (startY = 100; startY < 1000; startY += height / 2) {
      isConflict = false;
      for (int i = 0; i < num; i++) {
        if (site[i] > startY - height || site[i] < startY + height) {
          isConflict = true;
          break;
        }
      }
      if (isConflict) break;
    }
    miniViewParams.y = startY;
    site[id] = startY;
    AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
  }

  // 设置监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicInteger yy = new AtomicInteger();
    miniView.getRoot().setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          yy.set((int) event.getRawY());
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          miniViewParams.y = site[id] + (int) event.getRawY() - yy.get();
          AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          int flipY = (int) (yy.get() - event.getRawY());
          if (flipY * flipY < 16) clientView.changeToSmall();
          else site[id] = miniViewParams.y;
      }
      return true;
    });
  }

}
