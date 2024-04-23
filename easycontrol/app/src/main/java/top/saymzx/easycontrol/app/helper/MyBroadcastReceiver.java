package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.Map;

import top.saymzx.easycontrol.app.adb.UsbChannel;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.tools.AdbTools;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class MyBroadcastReceiver extends BroadcastReceiver {

  public static final String ACTION_UPDATE_USB = "top.saymzx.easycontrol.app.UPDATE_USB";
  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
  public static final String ACTION_UPDATE_DEVICE_LIST = "top.saymzx.easycontrol.app.UPDATE_DEVICE_LIST";
  public static final String ACTION_CONTROL = "top.saymzx.easycontrol.app.CONTROL";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";

  private DeviceListAdapter deviceListAdapter;

  // 注册广播
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void register(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_UPDATE_USB);
    filter.addAction(ACTION_UPDATE_DEVICE_LIST);
    filter.addAction(ACTION_CONTROL);
    filter.addAction(ACTION_SCREEN_OFF);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED);
    else context.registerReceiver(this, filter);
  }

  public void unRegister(Context context) {
    context.unregisterReceiver(this);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) AppData.uiHandler.postDelayed(() -> onConnectUsb(context, intent), 1000);
    else if (ACTION_UPDATE_USB.equals(action) || ACTION_USB_PERMISSION.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) updateUSB();
    else if (ACTION_SCREEN_OFF.equals(action)) handleScreenOff();
    else if (ACTION_UPDATE_DEVICE_LIST.equals(action)) deviceListAdapter.update();
    else if (ACTION_CONTROL.equals(action)) handleControl(intent);
  }


  public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
    this.deviceListAdapter = deviceListAdapter;
  }

  private void handleScreenOff() {
    for (Device device : AdbTools.devicesList) Client.sendAction(device.uuid, "close", null, 0);
  }

  private void handleControl(Intent intent) {
    String action = intent.getStringExtra("action");
    String uuid = intent.getStringExtra("uuid");
    if (action == null || uuid == null) return;
    if (action.equals("runShell")) {
      String cmd = intent.getStringExtra("cmd");
      if (cmd == null) return;
      Client.sendAction(uuid, action, ByteBuffer.wrap(cmd.getBytes()), 0);
    } else Client.sendAction(uuid, action, null, 0);
  }

  // 请求USB设备权限
  @SuppressLint({"MutableImplicitPendingIntent", "UnspecifiedImmutableFlag"})
  private void onConnectUsb(Context context, Intent intent) {
    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (usbDevice == null || AppData.usbManager == null) return;
    if (!AppData.usbManager.hasPermission(usbDevice)) {
      Intent usbPermissionIntent = new Intent(ACTION_USB_PERMISSION);
      usbPermissionIntent.setPackage(AppData.applicationContext.getPackageName());
      PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 1, usbPermissionIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
      AppData.usbManager.requestPermission(usbDevice, permissionIntent);
    }
  }

  public synchronized void updateUSB() {
    if (AppData.usbManager == null) return;
    AdbTools.usbDevicesList.clear();
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
      UsbDevice usbDevice = entry.getValue();
      if (usbDevice == null) return;
      if (AppData.usbManager.hasPermission(usbDevice)) {
        // 有线设备使用序列号作为唯一标识符
        String uuid = usbDevice.getSerialNumber();
        if (uuid == null) return;
        // 若没有该设备，则新建设备
        Device device = AppData.dbHelper.getByUUID(uuid);
        if (device == null) {
          device = new Device(uuid, Device.TYPE_LINK);
          device.address = uuid;
          AppData.dbHelper.insert(device);
        }
        AdbTools.usbDevicesList.put(uuid, usbDevice);
      }
    }
    deviceListAdapter.update();
  }

  public synchronized void resetUSB() {
    if (AppData.usbManager == null) return;
    try {
      for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
        UsbDevice usbDevice = entry.getValue();
        if (usbDevice == null) return;
        if (AppData.usbManager.hasPermission(usbDevice)) new UsbChannel(usbDevice).close();
      }
    } catch (Exception ignored) {
    }
  }

}
