package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class CenterHelper {
  @SuppressLint("StaticFieldLeak")
  private static DeviceListAdapter deviceListAdapter;
  private static final String localUUID = AppData.setting.getLocalUUID();
  private static String lastIpv6Address = "";

  public static void initCenterHelper(DeviceListAdapter deviceListAdapter) {
    CenterHelper.deviceListAdapter = deviceListAdapter;
  }

  public static void checkCenter() {
    new Thread(() -> {
      try {
        String centerAddress = AppData.setting.getCenterAddress();
        if (centerAddress.equals("")) return;
        int adbPort = AppData.setting.getCenterAdbPort();
        ArrayList<String> ipv6AddressList = PublicTools.getIp().second;
        String nowIpv6Address = ipv6AddressList.size() > 0 ? ("[" + ipv6AddressList.get(0) + "]:" + adbPort) : "";
        // 若IP地址发生改变，则撤销旧IP地址
        if (!Objects.equals(lastIpv6Address, nowIpv6Address)) {
          deleteDevice(centerAddress);
          lastIpv6Address = nowIpv6Address;
        }
        // 上报本机地址
        if (adbPort != -1 && !lastIpv6Address.equals("")) postDevice(centerAddress);
        // 获取设备列表
        deviceListAdapter.centerDevices.clear();
        if (!lastIpv6Address.equals("")) getDevice(centerAddress);
        AppData.main.runOnUiThread(deviceListAdapter::update);
      } catch (Exception e) {
        Log.e("Easycontrol", String.valueOf(e));
        AppData.main.runOnUiThread(() -> Toast.makeText(AppData.main, "Center:" + e, Toast.LENGTH_SHORT).show());
      }
    }).start();
  }

  // 撤销之前上报的地址
  private static void deleteDevice(String centerAddress) throws JSONException, IOException {
    JSONObject deleteDevice = createCenterPacket(NetHelper.DELETE_DEVICE);
    deleteDevice.put("uuid", localUUID);
    NetHelper.getJson(centerAddress, deleteDevice);
  }

  // 上报本机地址
  private static void postDevice(String centerAddress) throws JSONException, IOException {
    JSONObject postDevice = createCenterPacket(NetHelper.POST_DEVICE);
    postDevice.put("uuid", localUUID);
    postDevice.put("ip", lastIpv6Address);
    NetHelper.getJson(centerAddress, postDevice);
  }

  // 获取设备列表
  private static void getDevice(String centerAddress) throws JSONException, IOException {
    JSONObject getDevice = NetHelper.getJson(centerAddress, createCenterPacket(NetHelper.GET_DEVICE));
    JSONArray deviceArray = getDevice.getJSONArray("deviceArray");
    for (int i = 0; i < deviceArray.length(); i++) {
      JSONObject jsonObject = deviceArray.getJSONObject(i);
      String uuid = jsonObject.getString("uuid");
      // 跳过本机
      if (uuid.equals(localUUID)) continue;
      String ip = jsonObject.getString("ip");
      Device device = AppData.dbHelper.getByUUID(uuid);
      // 更新该设备地址
      if (device != null) {
        device.address = ip;
        AppData.dbHelper.update(device);
      }
      // 若没有该设备，则新建设备
      else {
        device = Device.getDefaultDevice(uuid, Device.TYPE_CENTER);
        device.address = ip;
        AppData.dbHelper.insert(device);
      }
      deviceListAdapter.centerDevices.add(uuid);
    }
  }

  private static JSONObject createCenterPacket(int handle) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("version", 3);
    jsonObject.put("easycontrol_name", AppData.setting.getCenterName());
    jsonObject.put("easycontrol_password", AppData.setting.getCenterPassword());
    jsonObject.put("handle", handle);
    return jsonObject;
  }

}
