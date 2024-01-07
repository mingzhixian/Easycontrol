package top.saymzx.easycontrol.app.helper;

import android.content.Intent;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import top.saymzx.buffer.Stream;
import top.saymzx.easycontrol.app.MainActivity;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class CloudHelper {
  private static final Double VERSION = 1.0;
  private static boolean reTry = true;
  private static String cloudAddress;
  private static String cloudName;
  private static String cloudPassword;
  private static int localAdbPort;
  private static final String localUUID = AppData.setting.getLocalUUID();
  public static ArrayList<String> cloudDevices = new ArrayList<>();

  private static Socket socket = null;
  private static DataInputStream dataInputStream = null;
  private static OutputStream outputStream = null;
  private static final ConcurrentHashMap<String, Stream> connectionStreams = new ConcurrentHashMap<>();

  private static final Thread handleInThread = new Thread(CloudHelper::handleIn);

  // 变量
  private static final int CLIENT_DEVICE_LOGIN = 1; // 客户端登陆，包含用户名密码、设备UUID、ip地址等信息
  private static final int SERVER_LOGIN_INFO = 2; // 服务器回复登陆信息，包含是否成功、参数限制等
  private static final int SERVER_DEVICE_UPDATE = 3; // 服务器发送该账号所有设备信息，包含设备UUID、地址等
  private static final int SERVER_KEEPALIVE = 4; // 服务器发送心跳包，用来确认客户端是否健在
  private static final int TUNNEL = 5; // 隧道包，包含源目的设备UUID等信息
  private static final int TUNNEL_CLOSE = 6; // 隧道断开包，包含源目的设备UUID等信息

  // 启动
  public static void start() {
    new Thread(() -> {
      stop();
      // 更新数据
      cloudAddress = AppData.setting.getCloudAddress();
      cloudName = AppData.setting.getCloudName();
      cloudPassword = AppData.setting.getCloudPassword();
      localAdbPort = AppData.setting.getLocalAdbPort();
      if (Objects.equals(cloudAddress, "") || Objects.equals(cloudName, "") || Objects.equals(cloudPassword, "")) return;
      // 连接
      try {
        Pair<String, Integer> address = PublicTools.getIpAndPort(cloudAddress);
        socket = new Socket(address.first, address.second);
        dataInputStream = new DataInputStream(socket.getInputStream());
        outputStream = socket.getOutputStream();
        sendLogin();
      } catch (IOException | org.json.JSONException e) {
        AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(R.string.error_cloud_error) + e, Toast.LENGTH_SHORT).show());
        return;
      }
      // 启动后台线程
      handleInThread.start();
    }).start();
  }

  public static void stop() {
    // 终止旧程序
    handleInThread.interrupt();
    // 断开连接
    try {
      if (socket != null) {
        dataInputStream.close();
        outputStream.close();
        socket.close();
        dataInputStream = null;
        outputStream = null;
        socket = null;
      }
    } catch (IOException ignored) {
    }
  }

  private static void handleIn() {
    try {
      while (!Thread.interrupted()) {
        switch (dataInputStream.readByte()) {
          case SERVER_LOGIN_INFO:
            handleLoginInfo();
            break;
          case SERVER_DEVICE_UPDATE:
            handleDeviceUpdate();
            break;
          case SERVER_KEEPALIVE:
            break;
          case TUNNEL:
            handleTunnel();
            break;
          case TUNNEL_CLOSE:
            handleTunnelClose();
            break;
        }
      }
    } catch (Exception ignored) {
      if (reTry) start();
    }
  }

  private synchronized static void write(byte[] data) throws IOException {
    outputStream.write(data);
  }

  private static void sendLogin() throws JSONException, IOException {
    Pair<ArrayList<String>, ArrayList<String>> ipAddressList = PublicTools.getIp();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("version", VERSION);
    jsonObject.put("cloudName", cloudName);
    jsonObject.put("cloudPassword", cloudPassword);
    jsonObject.put("uuid", localUUID);
    jsonObject.put("ipv4", ipAddressList.first.size() > 0 ? ipAddressList.first.get(0) : "");
    jsonObject.put("ipv6", ipAddressList.second.size() > 0 ? ipAddressList.second.get(0) : "");
    jsonObject.put("adbPort", localAdbPort);
    byte[] jsonByte = jsonObject.toString().getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(5 + jsonByte.length);
    buffer.put((byte) CLIENT_DEVICE_LOGIN);
    buffer.putInt(jsonByte.length);
    buffer.put(jsonByte);
    write(buffer.array());
  }

  private static void sendTunnel(String destination, byte[] data) throws IOException, JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("source", localUUID);
    jsonObject.put("destination", destination);
    jsonObject.put("len", data.length);
    byte[] jsonByte = jsonObject.toString().getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(5 + jsonByte.length + data.length);
    buffer.put((byte) TUNNEL);
    buffer.putInt(jsonByte.length);
    buffer.put(jsonByte);
    buffer.put(data);
    write(buffer.array());
  }

  private static void sendTunnelClose(String destination) throws JSONException, IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("source", localUUID);
    jsonObject.put("destination", destination);
    byte[] jsonByte = jsonObject.toString().getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(5 + jsonByte.length);
    buffer.put((byte) TUNNEL_CLOSE);
    buffer.putInt(jsonByte.length);
    buffer.put(jsonByte);
    write(buffer.array());
  }

  private static void handleLoginInfo() throws IOException, JSONException {
    JSONObject jsonObject = readHeader();
    boolean isSuccess = jsonObject.getBoolean("login");
    if (!isSuccess) {
      reTry = false;
      String msg = jsonObject.getString("msg");
      AppData.uiHandler.post(() -> Toast.makeText(AppData.main, msg, Toast.LENGTH_SHORT).show());
    } else {
      //todo 读取限制信息，用于限制客户端最大参数，防止超出服务器负载
    }
  }

  private static void handleDeviceUpdate() throws IOException, JSONException {
    cloudDevices.clear();
    JSONObject jsonObject = readHeader();
    JSONArray deviceArray = jsonObject.getJSONArray("deviceArray");
    for (int i = 0; i < deviceArray.length(); i++) {
      JSONObject deviceJson = deviceArray.getJSONObject(i);
      String uuid = jsonObject.getString("uuid");
      // 获取设备参数
      String ip;
      int adbPort = jsonObject.getInt("adbPort");
      String ipv4 = jsonObject.getString("ipv4");
      String ipv6 = jsonObject.getString("ipv6");
      if (uuid.equals(localUUID) || adbPort == -1) continue;
      // 检测连通性
      try {
        Socket testSocket = new Socket();
        testSocket.connect(new InetSocketAddress(ipv6, adbPort), 2000);
        testSocket.close();
        ip = ipv6 + ":" + adbPort;
      } catch (Exception ignored1) {
        try {
          Socket testSocket = new Socket();
          testSocket.connect(new InetSocketAddress(ipv4, adbPort), 2000);
          testSocket.close();
          ip = ipv4 + ":" + adbPort;
        } catch (Exception ignored2) {
          ip = "*" + uuid + "*";
        }
      }
      // 更新该设备地址
      Device device = AppData.dbHelper.getByUUID(uuid);
      if (device != null) {
        device.address = ip;
        AppData.dbHelper.update(device);
      } else {
        device = Device.getDefaultDevice(uuid, Device.TYPE_CLOUD);
        device.address = ip;
        AppData.dbHelper.insert(device);
      }
      cloudDevices.add(uuid);
      // 发送广播，更新设备列表显示
      AppData.main.sendBroadcast(new Intent(MainActivity.ACTION_DEVICE_LIST_UPDATE));
    }
  }

  private static void handleTunnel() throws IOException, JSONException {
    JSONObject jsonObject = readHeader();
    Stream stream = connectionStreams.get(jsonObject.getString("source"));
    if (stream != null) stream.pushSource(readByteArray(jsonObject.getInt("len")));
  }

  private static void handleTunnelClose() throws IOException, JSONException {
    JSONObject jsonObject = readHeader();
    Stream stream = connectionStreams.get(jsonObject.getString("source"));
    if (stream != null) stream.close();
  }

  private static JSONObject readHeader() throws IOException, JSONException {
    return new JSONObject(new String(readByteArray(dataInputStream.readInt())));
  }

  private static byte[] readByteArray(int len) throws IOException {
    byte[] buffer = new byte[len];
    dataInputStream.readFully(buffer);
    return buffer;
  }

  public Stream createNewTunnel(String destination) {
    Stream stream = new Stream(true, true, new Stream.UnderlySocketFunction() {
      @Override
      public void write(ByteBuffer buffer) throws Exception {
        sendTunnel(destination, buffer.array());
      }

      @Override
      public void close() {
        try {
          sendTunnelClose(destination);
        } catch (JSONException | IOException ignored) {
        }
      }
    });
    connectionStreams.put(destination, stream);
    return stream;
  }

}
