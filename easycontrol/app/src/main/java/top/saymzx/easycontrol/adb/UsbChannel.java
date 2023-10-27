package top.saymzx.easycontrol.adb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;

import top.saymzx.easycontrol.app.entity.AppData;

//此处代码摘抄借鉴了梦魇兽大佬的开源软件ADB KIT(https://github.com/nightmare-space/adb_kit/blob/main/android/app/src/main/java/com/nightmare/adbtools/adblib/UsbChannel.java)
public class UsbChannel implements AdbChannel {

  private final UsbDeviceConnection usbConnection;
  private UsbInterface usbInterface = null;
  private UsbEndpoint endpointIn = null;
  private UsbEndpoint endpointOut = null;

  public UsbChannel(UsbDevice usbDevice) throws IOException {
    // 连接USB设备
    usbConnection = AppData.usbManager.openDevice(usbDevice);
    // 查找ADB接口
    for (int i = 0; i < usbDevice.getInterfaceCount(); i++)
      // ADB协议的Class为VENDOR_SPEC(255)
      if ((usbInterface = usbDevice.getInterface(i)).getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC)
        break;
    // 宣告独占接口
    if (usbInterface != null && usbConnection.claimInterface(usbInterface, true)) {
      // 查找输入输出端点
      for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
        UsbEndpoint ep = usbInterface.getEndpoint(i);
        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
          if (ep.getDirection() == UsbConstants.USB_DIR_OUT) this.endpointOut = ep;
          else if (ep.getDirection() == UsbConstants.USB_DIR_IN) this.endpointIn = ep;
          if (endpointIn != null && endpointOut != null) return;
        }
      }
    }
    throw new IOException("有线连接错误");
  }

  @Override
  public void write(byte[] data) throws IOException {
    int size = data.length;
    int bytesWrite = 0;
    while (bytesWrite < size)
      bytesWrite += usbConnection.bulkTransfer(endpointOut, data, bytesWrite, size - bytesWrite, 1000);
  }

  @Override
  public void flush() {
  }

  @Override
  public byte[] read(int size) throws IOException {
    byte[] buffer = new byte[size];
    int bytesRead = 0;
    while (bytesRead < size) {
      bytesRead += usbConnection.bulkTransfer(endpointIn, buffer, bytesRead, size - bytesRead, 1000);
      Log.e("aaaa", String.valueOf(bytesRead));
    }
    return buffer;
  }

  @Override
  public void close() {
    try {
      usbConnection.releaseInterface(usbInterface);
      usbConnection.close();
    } catch (Exception ignored) {
    }
  }
}
