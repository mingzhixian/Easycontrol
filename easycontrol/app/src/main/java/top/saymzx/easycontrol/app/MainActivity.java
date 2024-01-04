package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import java.util.UUID;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ActivityMainBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.CloudHelper;
import top.saymzx.easycontrol.app.helper.DeviceListAdapter;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class MainActivity extends Activity {
  // 设备列表
  private DeviceListAdapter deviceListAdapter;

  // 需要启动默认设备
  private boolean needStartDefault = true;

  // 创建界面
  private ActivityMainBinding mainActivity;

  @SuppressLint("SourceLockedOrientationActivity")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainActivity = ActivityMainBinding.inflate(this.getLayoutInflater());
    setContentView(mainActivity.getRoot());
    AppData.init(this);
    // 设置状态栏导航栏颜色沉浸
    PublicTools.setStatusAndNavBar(this);
    // 设置设备列表适配器
    deviceListAdapter = new DeviceListAdapter(this);
    mainActivity.devicesList.setAdapter(deviceListAdapter);
    // 设置按钮监听
    setButtonListener();
    // 启动默认设备
    startDefault();
    // 注册广播监听
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_DEVICE_LIST_UPDATE);
    filter.addAction(ACTION_SCREEN_OFF);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
    else registerReceiver(broadcastReceiver, filter);
    // 启动Center检查服务
    startService(new Intent(this, CloudHelper.class));
  }

  @Override
  protected void onResume() {
    // 检查权限
    checkPermission();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(broadcastReceiver);
    super.onDestroy();
  }

  // 检查权限
  private void checkPermission() {
    // 检查悬浮窗权限
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
      intent.setData(Uri.parse("package:$packageName"));
      startActivity(intent);
      Toast.makeText(this, getString(R.string.main_float_permission), Toast.LENGTH_SHORT).show();
    }
  }

  // 启动默认设备
  private void startDefault() {
    if (needStartDefault && !AppData.setting.getDefaultDevice().equals("")) {
      needStartDefault = false;
      Device device = AppData.dbHelper.getByUUID(AppData.setting.getDefaultDevice());
      if (device != null) new Client(device);
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    mainActivity.buttonAdd.setOnClickListener(v -> PublicTools.createAddDeviceView(this, Device.getDefaultDevice(UUID.randomUUID().toString(), Device.TYPE_NORMAL), deviceListAdapter).show());
    mainActivity.buttonSet.setOnClickListener(v -> startActivity(new Intent(this, SetActivity.class)));
  }

  // 广播处理
  public static final String ACTION_DEVICE_LIST_UPDATE = "top.saymzx.easycontrol.app.DEVICE_LIST_UPDATE";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (ACTION_DEVICE_LIST_UPDATE.equals(intent.getAction())) deviceListAdapter.update();
      else if (ACTION_SCREEN_OFF.equals(intent.getAction())) handleScreenOff();
    }

    private void handleScreenOff() {
      for (Client client : Client.allClient) client.release();
    }
  };

}
