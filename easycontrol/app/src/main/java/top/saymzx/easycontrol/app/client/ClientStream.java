package top.saymzx.easycontrol.app.client;

import android.hardware.usb.UsbDevice;
import android.util.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.adb.Adb;
import top.saymzx.easycontrol.app.buffer.BufferStream;
import top.saymzx.easycontrol.app.client.decode.DecodecTools;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.entity.MyInterface;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientStream {
  private boolean isClose = false;
  private boolean connectDirect = false;
  private final boolean connectByUsb;
  private Adb adb;
  private Socket mainSocket;
  private Socket videoSocket;
  private OutputStream mainOutputStream;
  private DataInputStream mainDataInputStream;
  private DataInputStream videoDataInputStream;
  private BufferStream mainBufferStream;
  private BufferStream videoBufferStream;
  private BufferStream shell;
  private Thread connectThread = null;
  private static final String serverName = "/data/local/tmp/easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = DecodecTools.isSupportH265();
  private static final boolean supportOpus = DecodecTools.isSupportOpus();

  public ClientStream(Device device, UsbDevice usbDevice, MyInterface.MyFunctionBoolean handle) {
    connectByUsb = usbDevice != null;
    // 超时
    Thread timeOutThread = new Thread(() -> {
      try {
        Thread.sleep(10 * 1000);
        PublicTools.logToast("stream", AppData.applicationContext.getString(R.string.error_timeout), true);
        handle.run(false);
        if (connectThread != null) connectThread.interrupt();
      } catch (InterruptedException ignored) {
      }
    });
    // 连接
    connectThread = new Thread(() -> {
      try {
        Pair<String, Integer> address = null;
        if (!connectByUsb) address = PublicTools.getIpAndPort(device.address);
        adb = connectADB(address, usbDevice);
        startServer(device);
        connectServer(address);
        handle.run(true);
      } catch (Exception e) {
        PublicTools.logToast("stream", e.toString(), true);
        handle.run(false);
      } finally {
        timeOutThread.interrupt();
      }
    });
    connectThread.start();
    timeOutThread.start();
  }

  // 连接ADB
  private static Adb connectADB(Pair<String, Integer> address, UsbDevice usbDevice) throws Exception {
    if (address != null) return new Adb(address.first, address.second, AppData.keyPair);
    else return new Adb(usbDevice, AppData.keyPair);
  }

  // 启动Server
  private void startServer(Device device) throws Exception {
    if (BuildConfig.ENABLE_DEBUG_FEATURE || !adb.runAdbCmd("ls /data/local/tmp/easycontrol_*").contains(serverName)) {
      adb.runAdbCmd("rm /data/local/tmp/easycontrol_* ");
      adb.pushFile(AppData.applicationContext.getResources().openRawResource(R.raw.easycontrol_server), serverName);
    }
    shell = adb.getShell();
    shell.write(ByteBuffer.wrap(("app_process -Djava.class.path=" + serverName + " / top.saymzx.easycontrol.server.Server"
      + " isAudio=" + (device.isAudio ? 1 : 0) + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " keepAwake=" + (AppData.setting.getKeepAwake() ? 1 : 0)
      + " supportH265=" + ((device.useH265 && supportH265) ? 1 : 0)
      + " supportOpus=" + (supportOpus ? 1 : 0) + " \n").getBytes()));
  }

  // 连接Server
  private void connectServer(Pair<String, Integer> address) throws Exception {
    Thread.sleep(50);
    int reTry = 60;
    if (address != null) {
      reTry /= 2;
      for (int i = 0; i < reTry; i++) {
        try {
          if (mainSocket == null) mainSocket = new Socket(address.first, 25166);
          if (videoSocket == null) videoSocket = new Socket(address.first, 25166);
          mainOutputStream = mainSocket.getOutputStream();
          mainDataInputStream = new DataInputStream(mainSocket.getInputStream());
          videoDataInputStream = new DataInputStream(videoSocket.getInputStream());
          connectDirect = true;
          return;
        } catch (Exception ignored) {
          Thread.sleep(50);
        }
      }
    }
    // 直连失败尝试ADB中转
    for (int i = 0; i < reTry; i++) {
      try {
        if (mainBufferStream == null) mainBufferStream = adb.tcpForward(25166);
        // 为了减少adb同步阻塞的问题，此处分开音视频流
        if (videoBufferStream == null) videoBufferStream = adb.tcpForward(25166);
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.applicationContext.getString(R.string.error_connect_server));
  }

  public void runShell(String cmd) throws Exception {
    adb.runAdbCmd(cmd);
  }

  public byte readByteFromMain() throws IOException, InterruptedException {
    if (connectDirect) return mainDataInputStream.readByte();
    else return mainBufferStream.readByte();
  }

  public byte readByteFromVideo() throws IOException, InterruptedException {
    if (connectDirect) return videoDataInputStream.readByte();
    else return videoBufferStream.readByte();
  }

  public int readIntFromMain() throws IOException, InterruptedException {
    if (connectDirect) return mainDataInputStream.readInt();
    else return mainBufferStream.readInt();
  }

  public int readIntFromVideo() throws IOException, InterruptedException {
    if (connectDirect) return videoDataInputStream.readInt();
    else return videoBufferStream.readInt();
  }

  public ByteBuffer readByteArrayFromMain(int size) throws IOException, InterruptedException {
    if (connectDirect) {
      byte[] buffer = new byte[size];
      mainDataInputStream.readFully(buffer);
      return ByteBuffer.wrap(buffer);
    } else return mainBufferStream.readByteArray(size);
  }

  public ByteBuffer readByteArrayFromVideo(int size) throws IOException, InterruptedException {
    if (connectDirect) {
      byte[] buffer = new byte[size];
      videoDataInputStream.readFully(buffer);
      return ByteBuffer.wrap(buffer);
    }
    return videoBufferStream.readByteArray(size);
  }

  public ByteBuffer readFrameFromMain() throws Exception {
    if (!connectByUsb && !connectDirect) mainBufferStream.flush();
    return readByteArrayFromMain(readIntFromMain());
  }

  public ByteBuffer readFrameFromVideo() throws Exception {
    if (!connectByUsb && !connectDirect) videoBufferStream.flush();
    int size = readIntFromVideo();
    return readByteArrayFromVideo(size);
  }

  public void writeToMain(ByteBuffer byteBuffer) throws Exception {
    if (connectDirect) mainOutputStream.write(byteBuffer.array());
    else mainBufferStream.write(byteBuffer);
  }

  public void close() {
    if (isClose) return;
    isClose = true;
    if (shell != null) PublicTools.logToast("server", new String(shell.readByteArrayBeforeClose().array()), false);
    if (connectDirect) {
      try {
        mainOutputStream.close();
        videoDataInputStream.close();
        mainDataInputStream.close();
        mainSocket.close();
        videoSocket.close();
      } catch (Exception ignored) {
      }
    }
    if (adb != null) adb.close();
  }

  public static void runOnceCmd(Device device, UsbDevice usbDevice, String cmd, MyInterface.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Pair<String, Integer> address = null;
        if (usbDevice == null) address = PublicTools.getIpAndPort(device.address);
        Adb adb = connectADB(address, usbDevice);
        adb.runAdbCmd(cmd);
        adb.close();
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static void restartOnTcpip(Device device, UsbDevice usbDevice, MyInterface.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Pair<String, Integer> address = null;
        if (usbDevice == null) address = PublicTools.getIpAndPort(device.address);
        Adb adb = connectADB(address, usbDevice);
        String output = adb.restartOnTcpip(5555);
        adb.close();
        handle.run(output.contains("restarting"));
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

}
