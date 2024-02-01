package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicInteger;

import top.saymzx.easycontrol.app.client.ClientController;
import top.saymzx.easycontrol.app.databinding.ModuleMiniViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class MiniView {

  private final Device device;

  // 迷你悬浮窗
  private final ModuleMiniViewBinding miniView = ModuleMiniViewBinding.inflate(LayoutInflater.from(AppData.applicationContext));
  private final WindowManager.LayoutParams miniViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
    PixelFormat.TRANSLUCENT
  );

  public MiniView(Device device) {
    this.device = device;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    miniViewParams.x = 0;
    // 设置监听控制
    setBarListener();
    setButtonListener();
  }

  public void show() {
    miniViewParams.y = device.mini_y;
    // 显示
    ViewTools.viewAnim(miniView.getRoot(), true, PublicTools.dp2px(-40f), 0, (isStart -> {
      if (isStart) {
        AppData.windowManager.addView(miniView.getRoot(), miniViewParams);
      }
    }));
  }

  public void hide() {
    try {
      AppData.windowManager.removeView(miniView.getRoot());
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
          device.mini_y = miniViewParams.y;
          AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
      }
      return true;
    });
  }

  // 设置按钮监听
  private void setButtonListener() {
    miniView.buttonSmall.setOnClickListener(v-> ClientController.handleControll(device.uuid, "changeToSmall", null));
    miniView.buttonFull.setOnClickListener(v-> ClientController.handleControll(device.uuid, "changeToFull", null));
  }

}
