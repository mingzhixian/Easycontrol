package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ActivityMainBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.DeviceListAdapter;

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
    AppData.publicTools.setStatusAndNavBar(this);
    // 设置设备列表适配器
    deviceListAdapter = new DeviceListAdapter(this, AppData.dbHelper.getAll());
    mainActivity.masterDevicesList.setAdapter(deviceListAdapter);
    // 添加按钮监听
    setAddDeviceListener();
    // 设置按钮监听
    setSetButtonListener();
    // 启动默认设备
    startDefault();
    // 注册USB广播监听
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    registerReceiver(usbReceiver, filter);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
    // 检查权限
    checkPermission();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    // 注销USB广播监听
    unregisterReceiver(usbReceiver);
    super.onDestroy();
  }

  // 检查权限
  private void checkPermission() {
    // 检查悬浮窗权限
    if (!Settings.canDrawOverlays(this)) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
      intent.setData(Uri.parse("package:$packageName"));
      startActivity(intent);
      Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
    }
  }

  // 启动默认设备
  private void startDefault() {
    if (needStartDefault && AppData.setting.getDefaultDevice() != -1) {
      needStartDefault = false;
      Device device = AppData.dbHelper.getById(AppData.setting.getDefaultDevice());
      if (device != null) new Client(device, null);
    }
  }

  // 添加设备监听
  private void setAddDeviceListener() {
    mainActivity.masterAdd.setOnClickListener(v -> {
      Dialog dialog = AppData.publicTools.createAddDeviceView(this, Device.getDefaultDevice(), deviceListAdapter);
      dialog.show();
    });
  }

  // 设置按钮监听
  private void setSetButtonListener() {
    mainActivity.masterSet.setOnClickListener(v -> startActivity(new Intent(this, SetActivity.class)));
  }

  // USB设备监听
  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
  private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
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
          mainActivity.linkedDevice.getRoot().setVisibility(View.GONE);
          break;
        }
        // 授权完成
        case ACTION_USB_PERMISSION: {
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            mainActivity.linkedDevice.getRoot().setVisibility(View.VISIBLE);
            // 设置监听
            mainActivity.linkedDevice.getRoot().setOnClickListener(view -> new Client(Device.getDefaultDevice(), usbDevice));
          }
          break;
        }
      }
    }
  };
}
