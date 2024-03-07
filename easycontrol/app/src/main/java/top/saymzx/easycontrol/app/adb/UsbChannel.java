/*
 * 本页大量借鉴学习了开源ADB库：https://github.com/wuxudong/flashbot/blob/master/adblib/src/main/java/com/cgutman/adblib/UsbChannel.java，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.app.adb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import top.saymzx.easycontrol.app.buffer.BufferNew;
import top.saymzx.easycontrol.app.entity.AppData;

public class UsbChannel implements AdbChannel {

  private final UsbDeviceConnection usbConnection;
  private UsbInterface usbInterface = null;
  private UsbEndpoint endpointIn = null;
  private UsbEndpoint endpointOut = null;
  private final BufferNew sourceBuffer = new BufferNew();
  private final Thread readBackgroundThread = new Thread(this::readBackground);
  private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<>();

  public UsbChannel(UsbDevice usbDevice) throws IOException {
    // 连接USB设备
    if (AppData.usbManager == null) throw new IOException("not have usbManager");
    usbConnection = AppData.usbManager.openDevice(usbDevice);
    if (usbConnection == null) return;
    // 查找ADB的接口
    for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
      UsbInterface tmpUsbInterface = usbDevice.getInterface(i);
      if ((tmpUsbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) && (tmpUsbInterface.getInterfaceSubclass() == 66) && (tmpUsbInterface.getInterfaceProtocol() == 1)) {
        usbInterface = tmpUsbInterface;
        break;
      }
    }
    if (usbInterface == null) return;
    // 宣告独占接口
    if (usbConnection.claimInterface(usbInterface, true)) {
      // 查找输入输出端点
      for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
        UsbEndpoint endpoint = usbInterface.getEndpoint(i);
        if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
          if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) endpointOut = endpoint;
          else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) endpointIn = endpoint;
          if (endpointIn != null && endpointOut != null) {
            readBackgroundThread.start();
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
      usbConnection.bulkTransfer(endpointOut, header, header.length, 1000);
      // 读取载荷
      int payloadLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
      if (payloadLength > 0) {
        byte[] payload = new byte[payloadLength];
        data.get(payload);
        usbConnection.bulkTransfer(endpointOut, payload, payload.length, 1000);
      }
    }
  }

  @Override
  public ByteBuffer read(int size) throws InterruptedException, IOException {
    return sourceBuffer.read(size);
  }

  private void readBackground() {
    try {
      while (!Thread.interrupted()) {
        // 读取头部
        ByteBuffer header = readRequest(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        if (header.remaining() < AdbProtocol.ADB_HEADER_LENGTH) throw new IOException("read error");
        sourceBuffer.write(header);
        // 读取载荷
        int payloadLength = header.getInt(12);
        if (payloadLength > 0) {
          ByteBuffer payload = readRequest(payloadLength);
          sourceBuffer.write(payload);
        }
      }
    } catch (IOException ignored) {
      sourceBuffer.close();
    }
  }

  private ByteBuffer readRequest(int len) throws IOException {
    // 获取Request
    UsbRequest request;
    if (mInRequestPool.isEmpty()) {
      request = new UsbRequest();
      request.initialize(usbConnection, endpointIn);
    } else request = mInRequestPool.removeFirst();
    ByteBuffer data = ByteBuffer.allocate(len);
    request.setClientData(data);
    // 加入异步请求
    if (!request.queue(data, len)) throw new IOException("fail to queue read UsbRequest");
    // 等待请求回应
    while (true) {
      UsbRequest wait = usbConnection.requestWait();
      if (wait == null) throw new IOException("Connection.requestWait return null");
      if (wait.getEndpoint() == endpointIn) {
        ByteBuffer clientData = (ByteBuffer) wait.getClientData();
        mInRequestPool.add(request);
        if (clientData == data) {
          data.flip();
          return data;
        }
      }
    }
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
    readBackgroundThread.interrupt();
    try {
      // 强制让adb执行错误，从而断开重连USB
      usbConnection.bulkTransfer(endpointOut, new byte[100], 100, 100);
    } catch (Exception ignored) {
    }
    try {
      usbConnection.releaseInterface(usbInterface);
      usbConnection.close();
    } catch (Exception ignored) {
    }
  }

}