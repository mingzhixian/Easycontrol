package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.Controller;
import top.saymzx.easycontrol.app.databinding.ActivityFullBinding;
import top.saymzx.easycontrol.app.entity.AppData;

public class FullActivity extends Activity {

  public ActivityFullBinding fullActivity;
  private static Controller controller;
  private static Client.MyFunctionInt myFunctionInt;
  @SuppressLint("StaticFieldLeak")
  public static FullActivity context;

  public static Pair<Integer, Integer> maxSize;
  public static boolean isShow = false;

  public static boolean isPortrait;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    context = this;
    // 全屏
    AppData.publicTools.setFullScreen(this);
    fullActivity.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    // 隐藏工具栏
    fullActivity.barView.setVisibility(View.GONE);
    // 旋转
    context.setRequestedOrientation(FullActivity.isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    // 按键监听
    setButtonListener();
    // 更新MaxSize
    maxSize = getScreenSizeWithoutDock();
    if (!FullActivity.isPortrait) maxSize = new Pair<>(maxSize.second, maxSize.first);
    // 更新Surface
    listenerSurfaceOk();
    fullActivity.surfaceView.setSurfaceTextureListener();
  }

  @Override
  protected void onPause() {
    // 退出页面时自动变为小窗
    if (!isChangingConfigurations() && isShow) myFunctionInt.handleEvent(Client.Event_CHANGE_SMALL);
    super.onPause();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Toast.makeText(AppData.main, "全屏状态会拦截返回", Toast.LENGTH_SHORT).show();
      return true;
    }
    // 为不影响主机功能，仅传送常用输入字符
    else if (keyCode > KeyEvent.KEYCODE_0 && keyCode < KeyEvent.KEYCODE_AT) {
      controller.sendKeyEvent(keyCode);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public static void show(Client.MyFunctionInt myFunctionInt, Controller controller, boolean isPortrait) {
    if (!isShow) {
      isShow = true;
      FullActivity.myFunctionInt = myFunctionInt;
      FullActivity.controller = controller;
      FullActivity.isPortrait = isPortrait;
      AppData.main.startActivity(new Intent(AppData.main, FullActivity.class));
    }
  }

  public static void hide() {
    if (isShow) {
      isShow = false;
      context.finish();
    }
  }

  public static void changeRotation() {
    FullActivity.isPortrait = !FullActivity.isPortrait;
    context.setRequestedOrientation(FullActivity.isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  }

  // 获取去除底部操作栏后的屏幕大小，用于修改分辨率使用
  public static Pair<Integer, Integer> getScreenSizeWithoutDock() {
    Pair<Integer, Integer> screenSize = AppData.publicTools.getScreenSize();
    // 保持竖向
    if (screenSize.first > screenSize.second)
      screenSize = new Pair<>(screenSize.second, screenSize.first);
    return new Pair<>(screenSize.first, screenSize.second - AppData.publicTools.dp2px(35f));
  }

  // 等待Surface准备好后更新输出Surface
  private void listenerSurfaceOk() {
    surfaceView = fullActivity.surfaceView;
    surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        myFunctionInt.handleEvent(Client.Event_UPDATE_SURFACE);
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {

      }
    });
  }

  // 设置按钮监听
  private void setButtonListener() {
    fullActivity.buttonBack.setOnClickListener(v -> controller.sendKeyEvent(4));
    fullActivity.buttonHome.setOnClickListener(v -> controller.sendKeyEvent(3));
    fullActivity.buttonSwitch.setOnClickListener(v -> controller.sendKeyEvent(187));
    fullActivity.buttonMini.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CHANGE_MINI));
    fullActivity.buttonFullExit.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CHANGE_SMALL));
    fullActivity.buttonClose.setOnClickListener(v -> myFunctionInt.handleEvent(Client.Event_CLOSE));
    fullActivity.buttonMore.setOnClickListener(v -> AppData.publicTools.changeBarViewAnim(fullActivity.barView, AppData.publicTools.dp2px(40f)));
  }

}