package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import top.saymzx.easycontrol.app.client.ClientController;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class MyBroadcastReceiver extends BroadcastReceiver {

  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
  public static final String ACTION_UPDATE_DEVICE_LIST = "top.saymzx.easycontrol.app.UPDATE_DEVICE_LIST";
  private static final String ACTION_CONTROL = "top.saymzx.easycontrol.app.CONTROL";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";

  private DeviceListAdapter deviceListAdapter;

  // 注册广播
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void register(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
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
    if (ACTION_SCREEN_OFF.equals(action)) handleScreenOff();
    else if (ACTION_UPDATE_DEVICE_LIST.equals(action)) deviceListAdapter.update();
    else if (ACTION_CONTROL.equals(action)) handleControl(context, intent);
    else handleUSB(context, intent);
  }

  public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
    this.deviceListAdapter = deviceListAdapter;
  }

  private void handleScreenOff() {
    for (Device device : deviceListAdapter.devicesList) ClientController.handleControll(device.uuid, "close", null);
  }

  private void handleControl(Context context, Intent intent) {
    String action = intent.getStringExtra("action");
    String uuid = intent.getStringExtra("uuid");
    if (action == null || uuid == null) return;
    if (action.equals("start")) deviceListAdapter.startByUUID(uuid);
    else if (action.equals("runShell")) {
      String cmd = intent.getStringExtra("cmd");
      if (cmd == null) return;
      ClientController.handleControll(uuid, action, ByteBuffer.wrap(cmd.getBytes()));
    } else ClientController.handleControll(uuid, action, null);
  }

  private void handleUSB(Context context, Intent intent) {
    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    String action = intent.getAction();
    if (usbDevice == null && action != null) return;
    if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) onConnectUsb(context, usbDevice);
    else if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_DETACHED)) onCutUsb(usbDevice);
    else if (Objects.equals(action, ACTION_USB_PERMISSION)) if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) onGetUsbPer(usbDevice);
  }

  // 检查已连接设备
  public void checkConnectedUsb(Context context) {
    if (AppData.usbManager == null) return;
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) onConnectUsb(context, entry.getValue());
  }

  // 请求USB设备权限
  private void onConnectUsb(Context context, UsbDevice usbDevice) {
    if (AppData.usbManager == null) return;
    Intent intent = new Intent(ACTION_USB_PERMISSION);
    intent.setPackage(AppData.applicationContext.getPackageName());
    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, intent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
    AppData.usbManager.requestPermission(usbDevice, permissionIntent);
  }

  // 当断开设备
  private void onCutUsb(UsbDevice usbDevice) {
    for (Map.Entry<String, UsbDevice> entry : deviceListAdapter.linkDevices.entrySet()) {
      UsbDevice tmp = entry.getValue();
      if (tmp.getVendorId() == usbDevice.getVendorId() && tmp.getProductId() == usbDevice.getProductId()) deviceListAdapter.linkDevices.remove(entry.getKey());
    }
    deviceListAdapter.update();
  }

  // 处理USB授权结果
  private void onGetUsbPer(UsbDevice usbDevice) {
    // 查找ADB的接口
    for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
      UsbInterface tmpUsbInterface = usbDevice.getInterface(i);
      if ((tmpUsbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) && (tmpUsbInterface.getInterfaceSubclass() == 66) && (tmpUsbInterface.getInterfaceProtocol() == 1)) {
        // 有线设备使用序列号作为唯一标识符
        String uuid = usbDevice.getSerialNumber();
        // 若没有该设备，则新建设备
        Device device = AppData.dbHelper.getByUUID(uuid);
        if (device == null) {
          device = new Device(uuid, Device.TYPE_LINK);
          AppData.dbHelper.insert(device);
        }
        deviceListAdapter.linkDevices.put(uuid, usbDevice);
        deviceListAdapter.update();
        break;
      }
    }
  }
}
