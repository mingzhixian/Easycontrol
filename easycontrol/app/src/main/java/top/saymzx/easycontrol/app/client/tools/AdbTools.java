package top.saymzx.easycontrol.app.client.tools;

import android.hardware.usb.UsbDevice;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import top.saymzx.easycontrol.app.adb.Adb;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.entity.MyInterface;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class AdbTools {
  private static final HashMap<String, Adb> allAdbConnect = new HashMap<>();
  public static final ArrayList<Device> devicesList = new ArrayList<>();
  public static final HashMap<String, UsbDevice> usbDevicesList = new HashMap<>();

  // 连接ADB
  public static Adb connectADB(Device device) throws Exception {
    String address = device.address;
    // 如果包含应用名，则需要要分割出地址
    if (address.contains("#")) address = address.split("#")[0];
    Adb adb = allAdbConnect.get(address);
    if (adb == null || adb.isClosed()) {
      if (device.isLinkDevice()) adb = new Adb(usbDevicesList.get(address), AppData.keyPair);
      else adb = new Adb(PublicTools.getIpAndPort(address), AppData.keyPair);
      allAdbConnect.put(address, adb);
    }
    return adb;
  }

  public static void runOnceCmd(Device device, String cmd, MyInterface.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device);
        adb.runAdbCmd(cmd);
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static void restartOnTcpip(Device device, MyInterface.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device);
        String output = adb.restartOnTcpip(5555);
        handle.run(output.contains("restarting"));
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static void pushFile(Device device, InputStream file, String fileName, MyInterface.MyFunctionInt handleProcess) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device);
        adb.pushFile(file, "/sdcard/Download/Easycontrol/" + fileName, handleProcess);
        handleProcess.run(100);
      } catch (Exception ignored) {
        handleProcess.run(-1);
      }
    }).start();
  }
}
