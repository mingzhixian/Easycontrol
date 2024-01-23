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

import java.util.Map;
import java.util.Objects;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class MyBroadcastReceiver extends BroadcastReceiver {

  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";
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
    else if (ACTION_CONTROL.equals(action)) handleControl(intent);
    else handleUSB(context, intent);
  }

  public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
    this.deviceListAdapter = deviceListAdapter;
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
          else if (action.equals("buttonPower")) client.controlPacket.sendPowerEvent();
          else if (action.equals("buttonLight")) client.controlPacket.sendLightEvent(1);
          else if (action.equals("buttonLightOff")) client.controlPacket.sendLightEvent(0);
          else if (action.equals("buttonBack")) client.controlPacket.sendKeyEvent(4, 0);
          else if (action.equals("buttonHome")) client.controlPacket.sendKeyEvent(3, 0);
          else if (action.equals("buttonSwitch")) client.controlPacket.sendKeyEvent(187, 0);
          else if (action.equals("buttonRotate")) client.controlPacket.sendRotateEvent();
          else if (action.equals("close")) client.release(null);
          return;
        }
      }
    }
  }

  private void handleUSB(Context context, Intent intent) {
    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    String action = intent.getAction();
    if (usbDevice == null && action != null) return;
    if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) onConnectUsb(context, usbDevice);
    else if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_DETACHED)) onCutUsb(usbDevice);
    else if (Objects.equals(action, ACTION_USB_PERMISSION)) if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) onGetUsbPer(usbDevice);
  }

  // 启动默认设备
  public void startDefault() {
    String defaultDevice = AppData.setting.getDefaultDevice();
    if (!defaultDevice.equals("")) {
      for (Device device : deviceListAdapter.devicesList) {
        if (Objects.equals(device.uuid, defaultDevice)) {
          deviceListAdapter.startDevice(device);
          // 返回桌面
          if (AppData.setting.getAutoBackOnStartDefault()) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            AppData.main.startActivity(home);
          }
        }
      }
    }
  }

  // 检查已连接设备
  public void checkConnectedUsb(Context context) {
    if (AppData.usbManager==null)return;
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) onConnectUsb(context, entry.getValue());
  }

  // 请求USB设备权限
  private void onConnectUsb(Context context, UsbDevice usbDevice) {
    if (AppData.usbManager==null)return;
    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
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
          device = Device.getDefaultDevice(uuid, Device.TYPE_LINK);
          AppData.dbHelper.insert(device);
        }
        deviceListAdapter.linkDevices.put(uuid, usbDevice);
        deviceListAdapter.update();
        break;
      }
    }
  }
}
