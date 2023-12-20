package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ActivityMainBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.CenterHelper;
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
    CenterHelper.initCenterHelper(deviceListAdapter);
    // 设置按钮监听
    setButtonListener();
    // 启动默认设备
    startDefault();
    // 注册广播监听
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_CENTER_SERVICE);
    filter.addAction(ACTION_SCREEN_OFF);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
    else registerReceiver(broadcastReceiver, filter);
    // 启动Center检查服务
    CenterHelper.checkCenter(null);
    alarmPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_CENTER_SERVICE), PendingIntent.FLAG_IMMUTABLE);
    ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000 * 60 * 5, alarmPendingIntent);
  }

  @Override
  protected void onResume() {
    // 检查权限
    checkPermission();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    // 注销广播监听
    ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(alarmPendingIntent);
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
  private static final String ACTION_CENTER_SERVICE = "top.saymzx.easycontrol.app.CENTER_SERVICE";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
  private PendingIntent alarmPendingIntent;
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (ACTION_CENTER_SERVICE.equals(intent.getAction())) CenterHelper.checkCenter(null);
      else if (ACTION_SCREEN_OFF.equals(intent.getAction())) handleScreenOff();
    }

    private void handleScreenOff() {
      for (Client client : Client.allClient) client.release(null);
    }
  };

}
