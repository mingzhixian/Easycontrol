package top.saymzx.easycontrol.adb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import top.saymzx.easycontrol.adb.buffer.BufferNew;
import top.saymzx.easycontrol.app.entity.AppData;

//此处代码摘抄借鉴了梦魇兽大佬的开源软件ADB KIT(https://github.com/nightmare-space/adb_kit/blob/main/android/app/src/main/java/com/nightmare/adbtools/adblib/UsbChannel.java)
public class UsbChannel implements AdbChannel {

  private final UsbDeviceConnection usbConnection;
  private UsbInterface usbInterface = null;
  private UsbEndpoint endpointIn = null;
  private UsbEndpoint endpointOut = null;

  private final LinkedList<UsbRequest> outRequestPool = new LinkedList<>();
  private final LinkedList<UsbRequest> inRequestPool = new LinkedList<>();

  private final BufferNew inBuffer = new BufferNew();
  private final Thread readSyncThread = new Thread(this::readSync);

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
    if (usbInterface != null && usbConnection.claimInterface(usbInterface, false)) {
      // 查找输入输出端点
      for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
        UsbEndpoint endpoint = usbInterface.getEndpoint(i);
        if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
          if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) endpointOut = endpoint;
          else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) endpointIn = endpoint;
          if (endpointIn != null && endpointOut != null) {
            readSyncThread.start();
            return;
          }
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
      bytesWrite += usbConnection.bulkTransfer(endpointOut, data, bytesWrite, size - bytesWrite, 2000);
  }

  @Override
  public void flush() {
  }

  @Override
  public byte[] read(int size) throws IOException {
    try {
      return inBuffer.read(size).array();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
//    byte[] buffer = new byte[size];
//    int bytesRead = 0;
//    while (bytesRead < size)
//      bytesRead += usbConnection.bulkTransfer(endpointIn, buffer, bytesRead, size - bytesRead, 2000);
//    return buffer;
  }

  @Override
  public void close() {
    try {
      readSyncThread.interrupt();
      usbConnection.releaseInterface(usbInterface);
      usbConnection.close();
    } catch (Exception ignored) {
    }
  }

  byte[] headerBuffer = null;
  byte[] dataBuffer = null;

  private void readSync() {
    queueHeaderBuffer();
    while (!Thread.interrupted()) {
      UsbRequest tmpRequest = usbConnection.requestWait();
      Log.e("aaaa","requestWait");
      if (tmpRequest == null) break;
      releaseOutRequest(tmpRequest.getEndpoint() == endpointIn ? 1 : 2, tmpRequest);
      if (tmpRequest.getClientData() == headerBuffer) {
        Log.e("aaaa","headerBuffer");
        inBuffer.write(ByteBuffer.wrap(headerBuffer));
        queueDataBuffer();
      } else if (tmpRequest.getClientData() == dataBuffer) {
        Log.e("aaaa","dataBuffer");
        inBuffer.write(ByteBuffer.wrap(dataBuffer));
        queueHeaderBuffer();
      }
    }
  }

  private void queueHeaderBuffer() {
    headerBuffer = new byte[AdbProtocol.ADB_HEADER_LENGTH];
    UsbRequest request = getRequest(1);
    request.setClientData(headerBuffer);
    request.queue(ByteBuffer.wrap(headerBuffer), headerBuffer.length);
  }

  private void queueDataBuffer() {
    int len = ByteBuffer.wrap(headerBuffer).get(12);
    if (len == 0) return;
    dataBuffer = new byte[len];
    UsbRequest request = getRequest(1);
    request.setClientData(dataBuffer);
    request.queue(ByteBuffer.wrap(dataBuffer), dataBuffer.length);
  }

  private UsbRequest getRequest(int type) {
    LinkedList<UsbRequest> requestPool = (type == 1) ? inRequestPool : outRequestPool;
    UsbEndpoint endpoint = (type == 1) ? endpointIn : endpointOut;
    synchronized (requestPool) {
      if (requestPool.isEmpty()) {
        UsbRequest request = new UsbRequest();
        request.initialize(usbConnection, endpoint);
        return request;
      } else {
        return requestPool.removeFirst();
      }
    }
  }

  public void releaseOutRequest(int type, UsbRequest request) {
    LinkedList<UsbRequest> requestPool = (type == 1) ? inRequestPool : outRequestPool;
    synchronized (requestPool) {
      requestPool.add(request);
    }
  }

}
