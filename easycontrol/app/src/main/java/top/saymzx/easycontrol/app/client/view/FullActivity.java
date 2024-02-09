package top.saymzx.easycontrol.app.client.view;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import java.nio.ByteBuffer;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.ClientController;
import top.saymzx.easycontrol.app.client.ControlPacket;
import top.saymzx.easycontrol.app.databinding.ActivityFullBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class FullActivity extends Activity implements SensorEventListener {
  private boolean isClose = false;
  private Device device;
  private ActivityFullBinding fullActivity;
  private boolean autoRotate;
  private boolean light = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ViewTools.setFullScreen(this);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    device = ClientController.getDevice(getIntent().getStringExtra("uuid"));
    if (device == null) return;
    ClientController.setFullView(device.uuid, this);
    // 初始化
    fullActivity.barView.setVisibility(View.GONE);
    setNavBarHide(AppData.setting.getShowNavBarOnConnect());
    autoRotate = AppData.setting.getAutoRotate();
    fullActivity.buttonAutoRotate.setImageResource(autoRotate ? R.drawable.un_rotate : R.drawable.rotate);
    // 按键监听
    setButtonListener();
    setKeyEvent();
    // 更新textureView
    fullActivity.textureViewLayout.addView(ClientController.getTextureView(device.uuid), 0);
    fullActivity.textureViewLayout.post(() -> updateMaxSize(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight()));
    // 页面自动旋转
    AppData.sensorManager.registerListener(this, AppData.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onPause() {
    AppData.sensorManager.unregisterListener(this);
    if (isChangingConfigurations()) fullActivity.textureViewLayout.removeView(ClientController.getTextureView(device.uuid));
    else if (!isClose) ClientController.handleControll(device.uuid, AppData.setting.getFullToMiniOnExit() ? "changeToMini" : "changeToSmall", ByteBuffer.wrap("changeToFull".getBytes()));
    super.onPause();
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
    fullActivity.textureViewLayout.post(() -> updateMaxSize(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight()));
    super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
  }

  @Override
  public void onBackPressed() {
    Toast.makeText(this, getString(R.string.error_refused_back), Toast.LENGTH_SHORT).show();
  }

  private void updateMaxSize(int w, int h) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    byteBuffer.putInt(w);
    byteBuffer.putInt(h);
    byteBuffer.flip();
    ClientController.handleControll(device.uuid, "updateMaxSize", byteBuffer);
  }

  public void hide() {
    try {
      isClose = true;
      fullActivity.textureViewLayout.removeView(ClientController.getTextureView(device.uuid));
      finish();
    } catch (Exception ignored) {
    }
  }

  // 获取去除底部操作栏后的屏幕大小，用于修改分辨率使用
  public static float getResolution() {
    DisplayMetrics screenSize = PublicTools.getScreenSize();
    int min = Math.min(screenSize.widthPixels, screenSize.heightPixels);
    int max = Math.max(screenSize.widthPixels, screenSize.heightPixels);
    return (float) min / (float) (max - PublicTools.dp2px(35f));
  }

  // 设置按钮监听
  private void setButtonListener() {
    fullActivity.buttonRotate.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonRotate", null));
    fullActivity.buttonBack.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonBack", null));
    fullActivity.buttonHome.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonHome", null));
    fullActivity.buttonSwitch.setOnClickListener(v -> ClientController.handleControll(device.uuid, "buttonSwitch", null));
    fullActivity.buttonNavBar.setOnClickListener(v -> {
      setNavBarHide(fullActivity.navBar.getVisibility() == View.GONE);
      changeBarView();
    });
    fullActivity.buttonMini.setOnClickListener(v -> ClientController.handleControll(device.uuid, "changeToMini", null));
    fullActivity.buttonSmall.setOnClickListener(v -> ClientController.handleControll(device.uuid, "changeToSmall", null));
    fullActivity.buttonClose.setOnClickListener(v -> ClientController.handleControll(device.uuid, "close", null));
    fullActivity.buttonLight.setOnClickListener(v -> {
      light = !light;
      fullActivity.buttonLight.setImageResource(light ? R.drawable.lightbulb_off : R.drawable.lightbulb);
      ClientController.handleControll(device.uuid, light ? "buttonLight" : "buttonLightOff", null);
      changeBarView();
    });
    fullActivity.buttonPower.setOnClickListener(v -> {
      ClientController.handleControll(device.uuid, "buttonPower", null);
      changeBarView();
    });
    fullActivity.buttonMore.setOnClickListener(v -> changeBarView());
    fullActivity.buttonAutoRotate.setOnClickListener(v -> {
      autoRotate = !autoRotate;
      AppData.setting.setAutoRotate(autoRotate);
      fullActivity.buttonAutoRotate.setImageResource(autoRotate ? R.drawable.un_rotate : R.drawable.rotate);
      changeBarView();
    });
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    fullActivity.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    fullActivity.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
  }

  private void changeBarView() {
    boolean toShowView = fullActivity.barView.getVisibility() == View.GONE;
    boolean isLandscape = lastOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    ViewTools.viewAnim(fullActivity.barView, toShowView, 0, PublicTools.dp2px(40f) * (isLandscape ? -1 : 1), (isStart -> {
      if (isStart && toShowView) fullActivity.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) fullActivity.barView.setVisibility(View.GONE);
    }));
  }

  private int lastOrientation = -1;

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (!autoRotate || Sensor.TYPE_ACCELEROMETER != sensorEvent.sensor.getType()) return;
    float[] values = sensorEvent.values;
    float x = values[0];
    float y = values[1];
    int newOrientation = lastOrientation;

    if (x > -3 && x < 3 && y >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    else if (y > -3 && y < 3 && x >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else if (y > -3 && y < 3 && x <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    else if (x > -3 && x < 3 && y <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

    if (lastOrientation != newOrientation) {
      lastOrientation = newOrientation;
      setRequestedOrientation(newOrientation);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  // 设置键盘监听
  private void setKeyEvent() {
    fullActivity.editText.requestFocus();
    fullActivity.editText.setInputType(InputType.TYPE_NULL);
    fullActivity.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
        ClientController.handleControll(device.uuid, "writeByteBuffer", ControlPacket.createKeyEvent(event.getKeyCode(), event.getMetaState()));
        return true;
      }
      return false;
    });
  }
}