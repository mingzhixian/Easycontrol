package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class CenterHelper {
  private static final Double VERSION = 4.0;
  private static final int POST_DEVICE = 1;
  private static final int GET_DEVICE = 2;
  @SuppressLint("StaticFieldLeak")
  private static DeviceListAdapter deviceListAdapter;
  private static final String localUUID = AppData.setting.getLocalUUID();

  public static void initCenterHelper(DeviceListAdapter deviceListAdapter) {
    CenterHelper.deviceListAdapter = deviceListAdapter;
  }

  public static void checkCenter(PublicTools.MyFunctionString handleFunction) {
    new Thread(() -> {
      try {
        // 获取基础参数
        String centerAddress = AppData.setting.getCenterAddress();
        if (centerAddress.equals("")) return;
        int adbPort = AppData.setting.getCenterAdbPort();
        Pair<ArrayList<String>, ArrayList<String>> ipAddressList = PublicTools.getIp();
        // 上报本机地址
        if (adbPort != -1) postDevice(centerAddress, ipAddressList, adbPort);
        // 获取设备列表
        getDevice(centerAddress, ipAddressList.second.size() > 0);
        // 结束
        if (handleFunction != null) handleFunction.run(AppData.main.getString(R.string.center_button_code));
      } catch (Exception e) {
        String error = String.valueOf(e);
        Log.e("Easycontrol", error);
        if (handleFunction != null) handleFunction.run(error);
      }
    }).start();
  }

  // 上报本机地址
  private static void postDevice(String centerAddress, Pair<ArrayList<String>, ArrayList<String>> ipAddressList, int adbPort) throws JSONException, IOException {
    JSONObject postDevice = createCenterPacket(POST_DEVICE);
    postDevice.put("uuid", localUUID);
    postDevice.put("ipv4", ipAddressList.first.size() > 0 ? ipAddressList.first.get(0) : "");
    postDevice.put("ipv6", ipAddressList.second.size() > 0 ? ipAddressList.second.get(0) : "");
    postDevice.put("adbPort", adbPort);
    NetHelper.getJson(centerAddress, postDevice);
  }

  // 获取设备列表
  private static void getDevice(String centerAddress, boolean canConnectIpv6) throws JSONException, IOException {
    // 获取设备列表
    JSONObject getDevice = NetHelper.getJson(centerAddress, createCenterPacket(GET_DEVICE));
    JSONArray deviceArray = getDevice.getJSONArray("deviceArray");
    // 遍历设备列表
    deviceListAdapter.centerDevices.clear();
    for (int i = 0; i < deviceArray.length(); i++) {
      JSONObject jsonObject = deviceArray.getJSONObject(i);
      String uuid = jsonObject.getString("uuid");
      // 跳过本机
      if (uuid.equals(localUUID)) continue;
      // 获取设备参数
      String ip;
      int adbPort = jsonObject.getInt("adbPort");
      String ipv4 = jsonObject.getString("ipv4");
      String ipv6 = jsonObject.getString("ipv6");
      Device device = AppData.dbHelper.getByUUID(uuid);
      // 获取最佳地址
      if (canConnectIpv6 && !ipv6.equals("")) ip = ipv6;
      else ip = ipv4;
      // 检测连通性
      try {
        Socket testSocket = new Socket();
        testSocket.connect(new InetSocketAddress(ip, adbPort), 2000);
        testSocket.close();
      } catch (Exception ignored) {
        continue;
      }
      // 更新该设备地址
      if (device != null) {
        device.address = ip + ":" + adbPort;
        AppData.dbHelper.update(device);
      } else {
        device = Device.getDefaultDevice(uuid, Device.TYPE_CENTER);
        device.address = ip + ":" + adbPort;
        AppData.dbHelper.insert(device);
      }
      deviceListAdapter.centerDevices.add(uuid);
    }
    AppData.handler.post(deviceListAdapter::update);
  }

  private static JSONObject createCenterPacket(int handle) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("version", VERSION);
    jsonObject.put("easycontrol_one_one", AppData.setting.getCenterName());
    jsonObject.put("easycontrol_two_two", AppData.setting.getCenterPassword());
    jsonObject.put("handle", handle);
    return jsonObject;
  }

}
