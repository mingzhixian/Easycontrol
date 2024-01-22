package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import java.util.UUID;

import top.saymzx.easycontrol.app.databinding.ActivityMainBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.DeviceListAdapter;
import top.saymzx.easycontrol.app.helper.MyBroadcastReceiver;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class MainActivity extends Activity {
  // 设备列表
  private DeviceListAdapter deviceListAdapter;

  // 创建界面
  private ActivityMainBinding mainActivity;

  // 广播
  private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();

  @SuppressLint("SourceLockedOrientationActivity")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    AppData.init(this);
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    mainActivity = ActivityMainBinding.inflate(this.getLayoutInflater());
    setContentView(mainActivity.getRoot());
    // 检测激活
    checkActive();
    // 设置设备列表适配器
    deviceListAdapter = new DeviceListAdapter(this);
    mainActivity.devicesList.setAdapter(deviceListAdapter);
    myBroadcastReceiver.setDeviceListAdapter(deviceListAdapter);
    // 设置按钮监听
    setButtonListener();
    // 注册广播监听
    myBroadcastReceiver.register(this);
    // 检查已连接设备
    myBroadcastReceiver.checkConnectedUsb(this);
    // 启动默认设备
    AppData.uiHandler.postDelayed(myBroadcastReceiver::startDefault, 1000);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    // 检查权限
    checkPermission();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    myBroadcastReceiver.unRegister(this);
    super.onDestroy();
  }

  // 检测激活
  private void checkActive() {
    if (!AppData.setting.getIsActive()) startActivity(new Intent(this, ActiveActivity.class));
  }

  // 检查权限
  private void checkPermission() {
    // 检查悬浮窗权限，防止某些设备如鸿蒙不兼容
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:$packageName"));
        startActivity(intent);
        Toast.makeText(this, getString(R.string.main_float_permission), Toast.LENGTH_SHORT).show();
      }
    } catch (Exception ignored) {
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    mainActivity.buttonAdd.setOnClickListener(v -> PublicTools.createAddDeviceView(this, Device.getDefaultDevice(UUID.randomUUID().toString(), Device.TYPE_NORMAL), deviceListAdapter).show());
    mainActivity.buttonSet.setOnClickListener(v -> startActivity(new Intent(this, SetActivity.class)));
  }

}