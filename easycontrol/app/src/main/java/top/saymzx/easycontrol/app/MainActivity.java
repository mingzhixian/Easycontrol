package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Pair;
import android.widget.Toast;

import java.util.UUID;

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
    // 注册USB广播监听
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_CENTER_SERVICE);
    registerReceiver(broadcastReceiver, filter);
    // 启动Center检查服务
    CenterHelper.checkCenter();
    alarmPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_CENTER_SERVICE), PendingIntent.FLAG_MUTABLE);
    ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000 * 60 * 10, alarmPendingIntent);
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
      Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
    }
  }

  // 启动默认设备
  private void startDefault() {
    if (needStartDefault && !AppData.setting.getDefaultDevice().equals("")) {
      needStartDefault = false;
      Device device = AppData.dbHelper.getByUUID(AppData.setting.getDefaultDevice());
      if (device != null) new Client(device, null);
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    mainActivity.buttonAdd.setOnClickListener(v -> PublicTools.createAddDeviceView(this, Device.getDefaultDevice(UUID.randomUUID().toString(), Device.TYPE_NORMAL), deviceListAdapter).show());
    mainActivity.buttonSet.setOnClickListener(v -> startActivity(new Intent(this, SetActivity.class)));
  }

  // 广播处理
  public static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
  public static final String ACTION_CENTER_SERVICE = "top.saymzx.easycontrol.app.CENTER_SERVICE";
  PendingIntent alarmPendingIntent;
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (ACTION_CENTER_SERVICE.equals(intent.getAction())) CenterHelper.checkCenter();
      else handleUSB(context, intent);
    }

    private void handleUSB(Context context, Intent intent) {
      UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice == null) return;
      switch (intent.getAction()) {
        // USB设备已插入
        case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
          PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
          AppData.usbManager.requestPermission(usbDevice, permissionIntent);
          break;
        }
        // USB设备已拔出
        case UsbManager.ACTION_USB_DEVICE_DETACHED: {
          deviceListAdapter.linkDevice = null;
          deviceListAdapter.update();
          break;
        }
        // 授权完成
        case ACTION_USB_PERMISSION: {
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            String uuid = UUID.fromString(usbDevice.getDeviceName() + usbDevice.getProductId()).toString();
            // 若没有该设备，则新建设备
            Device device = AppData.dbHelper.getByUUID(uuid);
            if (device == null) {
              device = Device.getDefaultDevice(uuid, Device.TYPE_LINK);
              AppData.dbHelper.insert(device);
            }
            deviceListAdapter.linkDevice = new Pair<>(uuid, usbDevice);
            deviceListAdapter.update();
          }
          break;
        }
      }
    }
  };

}
