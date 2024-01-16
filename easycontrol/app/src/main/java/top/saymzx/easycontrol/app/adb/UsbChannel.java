package top.saymzx.easycontrol.app.adb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import top.saymzx.easycontrol.app.entity.AppData;

public class UsbChannel implements AdbChannel {

  private final UsbDeviceConnection usbConnection;
  private UsbInterface usbInterface = null;
  private UsbEndpoint endpointIn = null;
  private UsbEndpoint endpointOut = null;

  public UsbChannel(UsbDevice usbDevice) throws IOException {
    // 连接USB设备
    usbConnection = AppData.usbManager.openDevice(usbDevice);
    // 查找ADB的接口
    for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
      UsbInterface tmpUsbInterface = usbDevice.getInterface(i);
      if ((tmpUsbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) && (tmpUsbInterface.getInterfaceSubclass() == 66) && (tmpUsbInterface.getInterfaceProtocol() == 1)) {
        usbInterface = tmpUsbInterface;
        break;
      }
    }
    // 宣告独占接口
    if (usbInterface != null && usbConnection.claimInterface(usbInterface, true)) {
      // 查找输入输出端点
      for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
        UsbEndpoint endpoint = usbInterface.getEndpoint(i);
        if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
          if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) endpointOut = endpoint;
          else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) endpointIn = endpoint;
          if (endpointIn != null && endpointOut != null) {
            return;
          }
        }
      }
    }
    throw new IOException("有线连接错误");
  }

  @Override
  public void write(ByteBuffer data) throws IOException {
    // 此处感谢群友：○_○ 的帮助，ADB通过USB连接时必须头部和载荷分开发送，否则会导致ADB连接重置（官方的实现真差劲，明明可以顺序读取的）
    while (data.remaining() > 0) {
      // 读取头部
      byte[] header = new byte[AdbProtocol.ADB_HEADER_LENGTH];
      data.get(header);
      usbConnection.bulkTransfer(endpointOut, header, header.length, 5000);
      // 读取载荷
      int payloadLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
      if (payloadLength > 0) {
        byte[] payload = new byte[payloadLength];
        data.get(payload);
        usbConnection.bulkTransfer(endpointOut, payload, payload.length, 5000);
      }
    }
  }

  @Override
  public ByteBuffer read(int size) {
    byte[] buffer = new byte[size];
    usbConnection.bulkTransfer(endpointIn, buffer, size, 5000);
    return ByteBuffer.wrap(buffer);
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
    try {
      // 强制让adb执行错误，从而断开重连USB
      usbConnection.bulkTransfer(endpointOut, new byte[40], 40, 2000);
      usbConnection.claimInterface(usbInterface, false);
      usbConnection.releaseInterface(usbInterface);
      usbConnection.close();
    } catch (Exception ignored) {
    }
  }

}