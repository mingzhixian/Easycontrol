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
import android.text.InputType;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ActivityFullBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class FullActivity extends Activity implements SensorEventListener {
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
    clientView.setFullView(this);
    // 初始时锁定当前方向
    if (lastOrientation == -1) setRotation(-1);
    // 全屏
    PublicTools.setFullScreen(this);
    // 隐藏工具栏
    fullActivity.barView.setVisibility(View.GONE);
    // 设置默认导航栏状态
    setNavBarHide(AppData.setting.getDefaultShowNavBar());
    // 按键监听
    setButtonListener();
    setKeyEvent();
    // 更新textureView
    fullActivity.textureViewLayout.addView(clientView.textureView, 0);
    fullActivity.textureViewLayout.post(() -> clientView.updateMaxSize(new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight())));
    // 主控端自动旋转
    if (AppData.setting.getMasterAudoRotation()) {
      SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
    // 被控端旋转跟随
    if (AppData.setting.getSlaveAudoRotation()) clientView.client.controller.sendRotateEvent(lastOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
  }

  @Override
  protected void onPause() {
    ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
    if (isChangingConfigurations()) fullActivity.textureViewLayout.removeView(clientView.textureView);
    else if (isShow) clientView.changeToMini();
    super.onPause();
  }


  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
    fullActivity.textureViewLayout.post(() -> clientView.updateMaxSize(new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight())));
    super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
  }

  @Override
  public void onBackPressed() {
    Toast.makeText(AppData.main, getString(R.string.error_refused_back), Toast.LENGTH_SHORT).show();
  }

  public static void show(ClientView cli) {
    if (!isShow) {
      isShow = true;
      clientView = cli;
      AppData.main.startActivity(new Intent(AppData.main, FullActivity.class));
    }
  }

  public void hide(boolean force) {
    try {
      if (force || isShow) {
        isShow = false;
        lastOrientation = -1;
        fullActivity.textureViewLayout.removeView(clientView.textureView);
        finish();
        clientView.setFullView(null);
        clientView = null;
      }
    } catch (Exception ignored) {
    }
  }

  // 获取去除底部操作栏后的屏幕大小，用于修改分辨率使用
  public static float getResolution() {
    return (float) AppData.realScreenSize.widthPixels / (float) (AppData.realScreenSize.heightPixels - PublicTools.dp2px(32f));
  }

  // 设置按钮监听
  private void setButtonListener() {
    fullActivity.buttonBack.setOnClickListener(v -> clientView.client.controller.sendKeyEvent(4, 0));
    fullActivity.buttonHome.setOnClickListener(v -> clientView.client.controller.sendKeyEvent(3, 0));
    fullActivity.buttonSwitch.setOnClickListener(v -> clientView.client.controller.sendKeyEvent(187, 0));
    fullActivity.buttonMini.setOnClickListener(v -> clientView.changeToMini());
    fullActivity.buttonFullExit.setOnClickListener(v -> clientView.changeToSmall());
    fullActivity.buttonClose.setOnClickListener(v -> clientView.client.release(null));
    fullActivity.buttonNavBar.setOnClickListener(v -> setNavBarHide(fullActivity.navBar.getVisibility() == View.GONE));
    fullActivity.buttonRotate.setOnClickListener(v -> setRotation(-2));
    fullActivity.buttonMore.setOnClickListener(v -> changeBarView());
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    fullActivity.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    fullActivity.buttonNavBar.setImageResource(isShow ? R.drawable.divide : R.drawable.equals);
  }

  private void changeBarView() {
    boolean toShowView = fullActivity.barView.getVisibility() == View.GONE;
    boolean isLandscape = lastOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    clientView.viewAnim(fullActivity.barView, toShowView, 0, PublicTools.dp2px(40f) * (isLandscape ? -1 : 1), (isStart -> {
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

  // 设置键盘监听
  private void setKeyEvent() {
    fullActivity.editText.requestFocus();
    fullActivity.editText.setInputType(InputType.TYPE_NULL);
    fullActivity.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN) clientView.client.controller.sendKeyEvent(event.getKeyCode(), event.getMetaState());
      return true;
    });
  }
}