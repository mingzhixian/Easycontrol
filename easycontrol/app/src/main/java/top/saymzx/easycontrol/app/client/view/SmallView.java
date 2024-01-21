package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.ControlPacket;
import top.saymzx.easycontrol.app.databinding.ModuleSmallViewBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class SmallView extends ViewOutlineProvider {
  private final ClientView clientView;
  private static int statusBarHeight = 0;
  private boolean LocalIsPortrait() {
    return AppData.main.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
  }
  private boolean LastLocalIsPortrait;
  private boolean RemoteIsPortrait = true;
  private boolean InitSize = false;
  private boolean InitPos = false;

  // 悬浮窗
  private final ModuleSmallViewBinding smallView = ModuleSmallViewBinding.inflate(LayoutInflater.from(AppData.main));
  private final WindowManager.LayoutParams smallViewParams =
    new WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
      LayoutParamsFlagFocus,
      PixelFormat.TRANSLUCENT
    );

  private static final int LayoutParamsFlagFocus = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
  private static final int LayoutParamsFlagNoFocus = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

  public SmallView(ClientView clientView) {
    this.clientView = clientView;
    smallViewParams.gravity = Gravity.START | Gravity.TOP;
    // 设置默认导航栏状态
    setNavBarHide(AppData.setting.getDefaultShowNavBar());
    // 设置监听控制
    setFloatVideoListener();
    setReSizeListener();
    setBarListener();
    // 设置圆角
    smallView.body.setOutlineProvider(this);
    smallView.body.setClipToOutline(true);
    // 设置窗口大小
    DisplayMetrics displayMetrics = AppData.main.getResources().getDisplayMetrics();
    int screenWidth = displayMetrics.widthPixels;
    int screenHeight = displayMetrics.heightPixels + statusBarHeight;
    int longEdge = Math.max(screenWidth, screenHeight);
    int shortEdge = Math.min(screenWidth, screenHeight);
    if (clientView.device.small_p_p_width == 0 | clientView.device.small_p_p_height == 0
            | clientView.device.small_p_l_width == 0 | clientView.device.small_p_l_height == 0
            | clientView.device.small_l_p_width == 0 | clientView.device.small_l_p_height == 0
            | clientView.device.small_l_l_width == 0 | clientView.device.small_l_l_height == 0) {
      clientView.device.small_p_p_width = shortEdge * 4 / 5;
      clientView.device.small_p_p_height = longEdge * 4 / 5;
      clientView.device.small_p_l_width = shortEdge * 4 / 5;
      clientView.device.small_p_l_height = longEdge * 4 / 5;
      clientView.device.small_l_p_width = longEdge * 4 / 5;
      clientView.device.small_l_p_height = shortEdge * 4 / 5;
      clientView.device.small_l_l_width = longEdge * 4 / 5;
      clientView.device.small_l_l_height = shortEdge * 4 / 5;
    }
    clientView.updateMaxSize(new Pair<>(shortEdge * 4 / 5, shortEdge * 4 / 5));
    // 设置窗口监听
    smallView.textureViewLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
      if (right == 0 || bottom == 0) return;
      if (!InitSize & (right == shortEdge * 4 / 5 | bottom == shortEdge * 4 / 5)) InitSize = true;
      if (!InitSize) return;

      if (clientView.device.small_p_p_x == 0 & clientView.device.small_p_p_y == 0 & right < bottom & LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_p_p_width, clientView.device.small_p_p_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_p_p_x = (shortEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_p_p_y = (longEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_p_l_x == 0 & clientView.device.small_p_l_y == 0 & right > bottom & LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_p_l_width, clientView.device.small_p_l_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_p_l_x = (shortEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_p_l_y = (longEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_l_p_x == 0 & clientView.device.small_l_p_y == 0 & right < bottom & !LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_l_p_width, clientView.device.small_l_p_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_l_p_x = (longEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_l_p_y = (shortEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_l_l_x == 0 & clientView.device.small_l_l_y == 0 & right > bottom & !LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_l_l_width, clientView.device.small_l_l_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_l_l_x = (longEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_l_l_y = (shortEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }

      boolean LocalIsPortrait = LocalIsPortrait();
      if (!InitPos | right < bottom != RemoteIsPortrait | LocalIsPortrait != LastLocalIsPortrait) {
        InitPos = true;
        LastLocalIsPortrait = LocalIsPortrait;
        if (right < bottom) {
          if (LocalIsPortrait) {
            smallViewParams.x = clientView.device.small_p_p_x;
            smallViewParams.y = clientView.device.small_p_p_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_p_p_width, clientView.device.small_p_p_height));
          } else {
            smallViewParams.x = clientView.device.small_l_p_x;
            smallViewParams.y = clientView.device.small_l_p_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_l_p_width, clientView.device.small_l_p_height));
          }
        } else {
          if (LocalIsPortrait) {
            smallViewParams.x = clientView.device.small_p_l_x;
            smallViewParams.y = clientView.device.small_p_l_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_p_l_width, clientView.device.small_p_l_height));
          } else {
            smallViewParams.x = clientView.device.small_l_l_x;
            smallViewParams.y = clientView.device.small_l_l_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_l_l_width, clientView.device.small_l_l_height));
          }
        }
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
      }
      RemoteIsPortrait = right < bottom;
    });
  }

  public void show() {
    // 初始化
    smallView.barView.setVisibility(View.GONE);
    // 设置监听
    setButtonListener(clientView.controlPacket);
    setKeyEvent(clientView.controlPacket);
    // 显示
    AppData.windowManager.addView(smallView.getRoot(), smallViewParams);
    smallView.textureViewLayout.addView(clientView.textureView, 0);
    clientView.viewAnim(smallView.getRoot(), true, 0, PublicTools.dp2px(40f), null);
  }

  public void hide() {
    try {
      smallView.textureViewLayout.removeView(clientView.textureView);
      AppData.windowManager.removeView(smallView.getRoot());
      AppData.dbHelper.update(clientView.device);
    } catch (Exception ignored) {
    }
  }

  // 获取默认宽高比，用于修改分辨率使用
  public static float getResolution() {
    return 0.55f;
  }

  // 设置焦点监听
  @SuppressLint("ClickableViewAccessibility")
  private void setFloatVideoListener() {
    boolean defaultMiniOnOutside = AppData.setting.getDefaultMiniOnOutside();
    smallView.getRoot().setOnTouchHandle(event -> {
      if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
        if (defaultMiniOnOutside) clientView.changeToMini();
        else if (smallViewParams.flags != LayoutParamsFlagNoFocus) {
          smallView.editText.clearFocus();
          smallViewParams.flags = LayoutParamsFlagNoFocus;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        }
      } else if (smallViewParams.flags != LayoutParamsFlagFocus) {
        smallView.editText.requestFocus();
        smallViewParams.flags = LayoutParamsFlagFocus;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
      }
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
    smallView.bar.setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          xx.set((int) event.getRawX());
          yy.set((int) event.getRawY());
          paramsX.set(smallViewParams.x);
          paramsY.set(smallViewParams.y);
          isFilp.set(false);
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
          // 拖动限制，避免拖到状态栏
          if (y < statusBarHeight + 10) return true;
          // 更新
          smallViewParams.x = paramsX.get() + flipX;
          smallViewParams.y = paramsY.get() + flipY;
          if (RemoteIsPortrait) {
            if (LocalIsPortrait()) {
              clientView.device.small_p_p_x = smallViewParams.x;
              clientView.device.small_p_p_y = smallViewParams.y;
            } else {
              clientView.device.small_l_p_x = smallViewParams.x;
              clientView.device.small_l_p_y = smallViewParams.y;
            }
          }
          else {
            if (LocalIsPortrait()) {
              clientView.device.small_p_l_x = smallViewParams.x;
              clientView.device.small_p_l_y = smallViewParams.y;
            } else {
              clientView.device.small_l_l_x = smallViewParams.x;
              clientView.device.small_l_l_y = smallViewParams.y;
            }
          }
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          if (!isFilp.get()) changeBarView();
          break;
      }
      return true;
    });
  }

  // 设置按钮监听
  private void setButtonListener(ControlPacket controlPacket) {
    smallView.buttonRotate.setOnClickListener(v -> controlPacket.sendRotateEvent());
    smallView.buttonBack.setOnClickListener(v -> controlPacket.sendKeyEvent(4, 0));
    smallView.buttonHome.setOnClickListener(v -> controlPacket.sendKeyEvent(3, 0));
    smallView.buttonSwitch.setOnClickListener(v -> controlPacket.sendKeyEvent(187, 0));
    smallView.buttonNavBar.setOnClickListener(v -> {
      setNavBarHide(smallView.navBar.getVisibility() == View.GONE);
      changeBarView();
    });
    smallView.buttonMini.setOnClickListener(v -> clientView.changeToMini());
    smallView.buttonFull.setOnClickListener(v -> clientView.changeToFull());
    smallView.buttonClose.setOnClickListener(v -> {
      hide();
      clientView.onClose.run();
    });
    smallView.buttonLight.setOnClickListener(v -> {
      controlPacket.sendLightEvent(1);
      changeBarView();
    });
    smallView.buttonLightOff.setOnClickListener(v -> {
      controlPacket.sendLightEvent(0);
      changeBarView();
    });
    smallView.buttonPower.setOnClickListener(v -> {
      controlPacket.sendPowerEvent();
      changeBarView();
    });
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    smallView.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    smallView.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
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
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        int sizeX = (int) (event.getRawX() - smallViewParams.x);
        int sizeY = (int) (event.getRawY() - smallViewParams.y);
        if (sizeX < minSize || sizeY < minSize) return true;
        clientView.updateMaxSize(new Pair<>(sizeX, sizeY));

        if (sizeX < sizeY) {
          if (LocalIsPortrait()){
            clientView.device.small_p_p_width = sizeX;
            clientView.device.small_p_p_height = sizeY;
          } else {
            clientView.device.small_l_p_width = sizeX;
            clientView.device.small_l_p_height = sizeY;
          }
        }
        else {
          if (LocalIsPortrait()){
            clientView.device.small_p_l_width = sizeX;
            clientView.device.small_p_l_height = sizeY;
          } else {
            clientView.device.small_l_l_width = sizeX;
            clientView.device.small_l_l_height = sizeY;
          }
        }
      }
      return true;
    });
  }

  // 设置键盘监听
  private void setKeyEvent(ControlPacket controlPacket) {
    smallView.editText.setInputType(InputType.TYPE_NULL);
    smallView.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN) controlPacket.sendKeyEvent(event.getKeyCode(), event.getMetaState());
      return true;
    });
  }

  @Override
  public void getOutline(View view, Outline outline) {
    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AppData.main.getResources().getDimension(R.dimen.round));
  }

  static {
    @SuppressLint("InternalInsetResource") int resourceId = AppData.main.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      statusBarHeight = AppData.main.getResources().getDimensionPixelSize(resourceId);
    }
  }

}
