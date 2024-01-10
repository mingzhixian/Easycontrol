package top.saymzx.easycontrol.app.client;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import top.saymzx.adb.Adb;
import top.saymzx.buffer.Stream;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientStream {
  private static Handler handler;

  private Adb adb;
  private Stream tunnelStream;
  private Socket normalsocket;
  private OutputStream normalOutputStream;
  private DataInputStream normalInputStream;

  public static final String serverName = "/data/local/tmp/easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");

  public ClientStream(Device device, PublicTools.MyFunctionBoolean handle) {
    if (handler == null) {
      HandlerThread handlerThread = new HandlerThread("easycontrol_clientStream");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
    }
    new Thread(() -> {
      try {
        // 解析地址
        Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
        // 启动server
        startServer(device, address);
        // 连接server
        connectServer(device, address);
        handle.run(true);
      } catch (Exception e) {
        PublicTools.logToast(e.toString());
        handle.run(false);
      }
    }).start();
  }

  // 启动Server
  private void startServer(Device device, Pair<String, Integer> address) throws Exception {
    adb = new Adb(address.first, address.second, AppData.keyPair);
    if (BuildConfig.ENABLE_DEBUG_FEATURE || !adb.runAdbCmd("ls /data/local/tmp/easycontrol_*", true).contains(serverName)) {
      adb.runAdbCmd("rm /data/local/tmp/easycontrol_*", true);
      adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), serverName);
    }
    adb.runAdbCmd("app_process -Djava.class.path=" + serverName + " / top.saymzx.easycontrol.server.Server"
      + " tcpPort=" + (address.second + 1)
      + " isAudio=" + (device.isAudio ? 1 : 0)
      + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " turnOffScreen=" + (device.turnOffScreen ? 1 : 0)
      + " autoLockAfterControl=" + (device.autoLockAfterControl ? 1 : 0)
      + " useH265=" + ((device.useH265 && supportH265) ? 1 : 0)
      + " useOpus=" + ((device.useOpus && supportOpus) ? 1 : 0) + " > /dev/null 2>&1 &", false);
  }

  // 连接Server
  private void connectServer(Device device, Pair<String, Integer> address) throws Exception {
    Thread.sleep(50);
    for (int i = 0; i < 60; i++) {
      try {
        if (device.useTunnel) tunnelStream = adb.tcpForward(address.second + 1);
        else {
          normalsocket = new Socket(address.first, address.second + 1);
          normalInputStream = new DataInputStream(normalsocket.getInputStream());
          normalOutputStream = normalsocket.getOutputStream();
        }
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  public byte readByte() throws IOException, InterruptedException {
    if (tunnelStream == null) return normalInputStream.readByte();
    else return tunnelStream.readByte();
  }

  public int readInt() throws IOException, InterruptedException {
    if (tunnelStream == null) return normalInputStream.readInt();
    else return tunnelStream.readInt();
  }

  public long readLong() throws IOException, InterruptedException {
    if (tunnelStream == null) return normalInputStream.readLong();
    else return tunnelStream.readLong();
  }

  public byte[] readByteArray(int size) throws IOException, InterruptedException {
    if (tunnelStream == null) {
      byte[] buffer = new byte[size];
      normalInputStream.readFully(buffer);
      return buffer;
    } else return tunnelStream.readByteArray(size).array();
  }

  public byte[] readFrame() throws IOException, InterruptedException {
    return readByteArray(readInt());
  }

  public void write(byte[] buffer) throws Exception {
    if (tunnelStream == null) {
      handler.post(() -> {
        try {
          normalOutputStream.write(buffer);
        } catch (IOException ignored) {
          close();
        }
      });
    } else tunnelStream.write(ByteBuffer.wrap(buffer));
  }

  public void close() {
    if (tunnelStream == null) {
      try {
        normalOutputStream.close();
        normalInputStream.close();
        normalsocket.close();
      } catch (IOException ignored) {
      }
    } else tunnelStream.close();
  }
}
