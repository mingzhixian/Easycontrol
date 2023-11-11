package top.saymzx.easycontrol.center;

import java.util.ArrayList;
import java.util.Objects;

public class User {
  public final String name;
  public String password;
  public final ArrayList<Device> devices = new ArrayList<>();

  public User(String name, String password) {
    this.name = name;
    this.password = password;
  }

  public Device getDevice(String deviceID) {
    for (Device device : devices) {
      if (Objects.equals(device.deviceID, deviceID)) {
        return device;
      }
    }
    return null;
  }

  static class Device {
    public final String deviceID;
    public String ip;
    public long lastPostTime;

    public Device(String deviceID, String ip) {
      this.deviceID = deviceID;
      this.ip = ip;
    }
  }
}
