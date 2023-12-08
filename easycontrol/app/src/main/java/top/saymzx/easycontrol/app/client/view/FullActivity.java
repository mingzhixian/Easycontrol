package top.saymzx.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

public class FullActivity extends Activity implements SensorEventListener {
  @SuppressLint("StaticFieldLeak")
  private static FullActivity context;
  private static Controller controller;
  @SuppressLint("StaticFieldLeak")
  private static ClientView clientView;

  private ActivityFullBinding fullActivity;
  private static int lastOrientation = -1;

  public static boolean isShow = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    context = this;
    // 初始时锁定当前方向
    if (lastOrientation == -1) setRotation(-1);
    // 全屏
    PublicTools.setFullScreen(this);
    // 隐藏工具栏
    fullActivity.barView.setVisibility(View.GONE);
    // 按键监听
    setButtonListener();
    // 更新textureView
    fullActivity.textureViewLayout.addView(clientView.textureView, 0);
    fullActivity.textureViewLayout.post(() -> clientView.updateMaxSize(new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight())));
    // 主控端自动旋转
    if (AppData.setting.getMasterAudoRotation().first) {
      SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
    // 被控端旋转跟随
    if (AppData.setting.getSlaveAudoRotation().first) controller.sendRotateEvent(lastOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
  }

  @Override
  protected void onPause() {
    ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
    fullActivity.textureViewLayout.removeView(clientView.textureView);
    // 非正常退出页面
    if (!isChangingConfigurations() && isShow) clientView.changeToMini();
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

  public static void show(ClientView cli, Controller con) {
    if (!isShow) {
      isShow = true;
      clientView = cli;
      controller = con;
      AppData.main.startActivity(new Intent(AppData.main, FullActivity.class));
    }
  }

  public static void hide() {
    if (isShow) {
      isShow = false;
      lastOrientation = -1;
      context.finish();
      context = null;
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
    fullActivity.buttonRotate.setOnClickListener(v -> setRotation(-2));
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

  // 设置页面方向
  private void setRotation(int rotation) {
    // 设置-1为锁定当前方向
    if (rotation == -1) {
      rotation = getResources().getConfiguration().orientation == 1 ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }
    // 设置-2为反转当前方向
    else if (rotation == -2) {
      rotation = getResources().getConfiguration().orientation == 1 ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }
    lastOrientation = rotation;
    setRequestedOrientation(rotation);
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (Sensor.TYPE_ACCELEROMETER != sensorEvent.sensor.getType()) return;
    float[] values = sensorEvent.values;
    float x = values[0];
    float y = values[1];
    int newOrientation = lastOrientation;

    if (x > -3 && x < 3 && y >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    else if (y > -3 && y < 3 && x >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else if (y > -3 && y < 3 && x <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    else if (x > -3 && x < 3 && y <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

    if (lastOrientation != newOrientation) setRotation(newOrientation);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }
}