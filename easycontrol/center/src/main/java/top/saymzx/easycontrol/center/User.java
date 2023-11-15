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

  public Device getDevice(String uuid) {
    for (Device device : devices) if (Objects.equals(device.uuid, uuid)) return device;
    return null;
  }

  static class Device {
    public final String uuid;
    public String ip;
    public long lastPostTime;

    public Device(String uuid, String ip) {
      this.uuid = uuid;
      this.ip = ip;
    }
  }
}
