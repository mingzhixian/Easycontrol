package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import top.saymzx.easycontrol.app.adb.UsbChannel;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.tools.AdbTools;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class MyBroadcastReceiver extends BroadcastReceiver {

  public static final String ACTION_UPDATE_USB = "top.saymzx.easycontrol.app.UPDATE_USB";
  public static final String ACTION_UPDATE_DEVICE_LIST = "top.saymzx.easycontrol.app.UPDATE_DEVICE_LIST";
  public static final String ACTION_CONTROL = "top.saymzx.easycontrol.app.CONTROL";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";

  private DeviceListAdapter deviceListAdapter;

  // 注册广播
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void register(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
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
    if (ACTION_UPDATE_USB.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) updateUSB();
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

  public synchronized void updateUSB() {
    if (AppData.usbManager == null) return;
    AdbTools.usbDevicesList.clear();
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
      UsbDevice usbDevice = entry.getValue();
      if (AppData.usbManager.hasPermission(usbDevice)) {
        // 有线设备使用序列号作为唯一标识符
        String uuid = usbDevice.getSerialNumber();
        // 若没有该设备，则新建设备
        Device device = AppData.dbHelper.getByUUID(uuid);
        if (device == null) {
          device = new Device(uuid, Device.TYPE_LINK);
          AppData.dbHelper.insert(device);
        }
        device.address = uuid + (device.address.contains("#") ? device.address.split("#")[1] : "");
        AdbTools.usbDevicesList.put(uuid, usbDevice);
        break;
      }
    }
    deviceListAdapter.update();
  }

  public synchronized void resetUSB() {
    if (AppData.usbManager == null) return;
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
      UsbDevice usbDevice = entry.getValue();
      if (AppData.usbManager.hasPermission(usbDevice)) {
        try {
          new UsbChannel(usbDevice).close();
        } catch (IOException ignored) {
        }
      }
    }
  }

}
