package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.provider.Settings;
import android.util.Pair;
import android.widget.Toast;

import java.util.Objects;
import java.util.UUID;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ActivityMainBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.DeviceListAdapter;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class MainActivity extends Activity {
  // 设备列表
  private DeviceListAdapter deviceListAdapter;

  // 创建界面
  private ActivityMainBinding mainActivity;

  @SuppressLint("SourceLockedOrientationActivity")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    AppData.init(this);
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    mainActivity = ActivityMainBinding.inflate(this.getLayoutInflater());
    setContentView(mainActivity.getRoot());
    // 设置设备列表适配器
    deviceListAdapter = new DeviceListAdapter(this);
    mainActivity.devicesList.setAdapter(deviceListAdapter);
    // 设置按钮监听
    setButtonListener();
    // 启动默认设备
    startDefault();
    // 注册广播监听
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_CONTROL);
    filter.addAction(ACTION_SCREEN_OFF);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
    else registerReceiver(broadcastReceiver, filter);
    // 检查USB
    for (String k : AppData.usbManager.getDeviceList().keySet()) {
      UsbDevice device = AppData.usbManager.getDeviceList().get(k);
      if (AppData.usbManager.hasPermission(device)) {
        String uuid = device.getSerialNumber();
        Device d = AppData.dbHelper.getByUUID(uuid);
        if (d == null) {
          d = Device.getDefaultDevice(uuid, Device.TYPE_LINK);
          AppData.dbHelper.insert(d);
        }
        deviceListAdapter.linkDevice = new Pair<>(uuid, device);
        deviceListAdapter.update();
      } else {
        AppData.usbManager.requestPermission(
                device,
                PendingIntent.getBroadcast(getApplicationContext(),
                        0,
                        new Intent("top.saymzx.easycontrol.app.USB_PERMISSION"),
                        PendingIntent.FLAG_MUTABLE));
      }
      break; // 只处理第一个设备
    }
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
    unregisterReceiver(broadcastReceiver);
    super.onDestroy();
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

  // 启动默认设备
  private void startDefault() {
    String defaultDevice = AppData.setting.getDefaultDevice();
    if (!defaultDevice.equals("")) {
      Device device = AppData.dbHelper.getByUUID(defaultDevice);
      if (device != null && device.isNormalDevice()) {
        new Client(device, null);
        // 返回桌面
        if (AppData.setting.getAutoBackOnStartDefault()) {
          Intent home = new Intent(Intent.ACTION_MAIN);
          home.addCategory(Intent.CATEGORY_HOME);
          AppData.main.startActivity(home);
        }
      }
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    mainActivity.buttonAdd.setOnClickListener(v -> PublicTools.createAddDeviceView(this, Device.getDefaultDevice(UUID.randomUUID().toString(), Device.TYPE_NORMAL), deviceListAdapter).show());
    mainActivity.buttonSet.setOnClickListener(v -> startActivity(new Intent(this, SetActivity.class)));
  }

  // 广播处理
  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
  private static final String ACTION_CONTROL = "top.saymzx.easycontrol.app.CONTROL";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_SCREEN_OFF.equals(action)) handleScreenOff();
      else if (ACTION_CONTROL.equals(action)) handleControl(intent);
      else handleUSB(context, intent);
    }

    private void handleScreenOff() {
      for (Client client : Client.allClient) client.release(null);
    }

    private void handleControl(Intent intent) {
      String action = intent.getStringExtra("action");
      if (action == null) return;
      if (action.equals("startDefault")) {
        startDefault();
        return;
      }
      String uuid = intent.getStringExtra("uuid");
      if (uuid == null) return;
      if (action.equals("start")) deviceListAdapter.startByUUID(uuid);
      else {
        for (Client client : Client.allClient) {
          if (Objects.equals(client.uuid, uuid)) {
            if (action.equals("changeToSmall")) client.clientView.changeToSmall();
            else if (action.equals("changeToFull")) client.clientView.changeToFull();
            else if (action.equals("changeToMini")) client.clientView.changeToMini();
            else if (action.equals("close")) client.release(null);
            return;
          }
        }
      }
    }

    private void handleUSB(Context context, Intent intent) {
      UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice == null) return;
      switch (Objects.requireNonNull(intent.getAction())) {
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
            // 有线设备使用序列号作为唯一标识符
            String uuid = usbDevice.getSerialNumber();
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