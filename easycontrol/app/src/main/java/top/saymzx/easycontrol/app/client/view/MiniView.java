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
  private final WindowManager.LayoutParams miniViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
    PixelFormat.TRANSLUCENT
  );

  private static int num = 0;

  public MiniView(ClientView clientView) {
    this.clientView = clientView;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    miniViewParams.x = 0;
    // Bar颜色
    int colorNum = num++ % 4;
    int barColor = R.color.bar1;
    if (colorNum == 1) barColor = R.color.bar2;
    else if (colorNum == 2) barColor = R.color.bar3;
    else if (colorNum == 3) barColor = R.color.bar4;
    miniView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(barColor)));
  }

  public void show() {
    miniViewParams.y = clientView.device.mini_y;
    // 设置监听控制
    setBarListener();
    // 显示
    clientView.viewAnim(miniView.getRoot(), true, PublicTools.dp2px(-40f), 0, (isStart -> {
      if (isStart) {
        miniView.getRoot().setVisibility(View.VISIBLE);
        AppData.windowManager.addView(miniView.getRoot(), miniViewParams);
      }
    }));
  }

  public void hide() {
    try {
      miniView.getRoot().setVisibility(View.GONE);
      AppData.windowManager.removeView(miniView.getRoot());
      AppData.dbHelper.update(clientView.device);
    } catch (Exception ignored) {
    }
  }

  // 设置监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger oldYy = new AtomicInteger();
    miniView.getRoot().setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          yy.set((int) event.getRawY());
          oldYy.set(miniViewParams.y);
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          miniViewParams.y = oldYy.get() + (int) event.getRawY() - yy.get();
          clientView.device.mini_y = miniViewParams.y;
          AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          int flipY = (int) (yy.get() - event.getRawY());
          if (flipY * flipY < 16) clientView.changeToSmall();
          break;
      }
      return true;
    });
  }

}
