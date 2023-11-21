package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Controller;
import top.saymzx.easycontrol.app.databinding.ActivityFullBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class FullActivity extends Activity {
  @SuppressLint("StaticFieldLeak")
  private static FullActivity context;
  private static Controller controller;
  @SuppressLint("StaticFieldLeak")
  private static ClientView clientView;

  private ActivityFullBinding fullActivity;

  public static boolean isShow = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    context = this;
    // 全屏
    PublicTools.setFullScreen(this);
    // 隐藏工具栏
    fullActivity.barView.setVisibility(View.GONE);
    // 按键监听
    setButtonListener();
    // 更新textureView
    fullActivity.textureViewLayout.addView(clientView.textureView, 0);
    fullActivity.textureViewLayout.post(() -> clientView.updateMaxSize(new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight())));
  }

  @Override
  protected void onPause() {
    // 旋转时及时删除
    if (isChangingConfigurations()) context.fullActivity.textureViewLayout.removeView(clientView.textureView);
      // 退出页面时自动变为小窗
    else if (isShow) clientView.changeToMini();
    super.onPause();
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
    fullActivity.textureViewLayout.post(() -> clientView.updateMaxSize(new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight())));
    super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Toast.makeText(AppData.main, "全屏状态会拦截返回", Toast.LENGTH_SHORT).show();
      return true;
    }
    // 为不影响主机功能，仅传送常用输入字符
    else if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT) || (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) || (keyCode >= KeyEvent.KEYCODE_COMMA && keyCode <= KeyEvent.KEYCODE_PLUS)) {
      controller.sendKeyEvent(keyCode);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public static void show(ClientView clientView, Controller controller) {
    if (!isShow) {
      isShow = true;
      FullActivity.clientView = clientView;
      FullActivity.controller = controller;
      AppData.main.startActivity(new Intent(AppData.main, FullActivity.class));
    }
  }

  public static void changeRotation() {
    context.setRequestedOrientation(AppData.rotationIsPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  }

  public static void hide() {
    if (isShow) {
      isShow = false;
      context.fullActivity.textureViewLayout.removeView(clientView.textureView);
      context.finish();
    }
  }

  // 获取去除底部操作栏后的屏幕大小，用于修改分辨率使用
  public static float getResolution() {
    return (float) AppData.realScreenSize.widthPixels / (float) (AppData.realScreenSize.heightPixels - PublicTools.dp2px(32f));
  }

  // 设置按钮监听
  private void setButtonListener() {
    fullActivity.buttonBack.setOnClickListener(v -> controller.sendKeyEvent(4));
    fullActivity.buttonHome.setOnClickListener(v -> controller.sendKeyEvent(3));
    fullActivity.buttonSwitch.setOnClickListener(v -> controller.sendKeyEvent(187));
    fullActivity.buttonMini.setOnClickListener(v -> clientView.changeToMini());
    fullActivity.buttonFullExit.setOnClickListener(v -> clientView.changeToSmall());
    fullActivity.buttonClose.setOnClickListener(v -> clientView.hide(true));
    fullActivity.buttonPower.setOnClickListener(v -> controller.sendPowerEvent());
    fullActivity.buttonNavBar.setOnClickListener(v -> setNavBarHide());
    fullActivity.buttonMore.setOnClickListener(v -> changeBarView());
  }

  // 导航栏隐藏
  private void setNavBarHide() {
    changeBarView();
    boolean isShow = fullActivity.navBar.getVisibility() == View.GONE;
    fullActivity.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    fullActivity.buttonNavBar.setImageResource(isShow ? R.drawable.hide_nav : R.drawable.show_nav);
  }

  private void changeBarView() {
    boolean toShowView = fullActivity.barView.getVisibility() == View.GONE;
    clientView.viewAnim(fullActivity.barView, toShowView, 0, PublicTools.dp2px(40f), (isStart -> {
      if (isStart && toShowView) fullActivity.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) fullActivity.barView.setVisibility(View.GONE);
    }));
  }

  // 更新模糊背景图
  public static void updateBackImage(Drawable drawable) {
    context.fullActivity.getRoot().setBackground(drawable);
  }

}