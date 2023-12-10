package top.saymzx.easycontrol.center;

import org.json.JSONObject;

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
    private final String uuid;
    private int adbPort;
    private String ipv6;
    private String ipv4;
    private long lastPostTime;

    public Device(String uuid) {
      this.uuid = uuid;
    }

    public void update(String ipv4, String ipv6, int adbPort, long lastPostTime) {
      this.ipv4 = ipv4;
      this.ipv6 = ipv6;
      this.adbPort = adbPort;
      this.lastPostTime = lastPostTime;
    }

    public JSONObject toJson() {
      JSONObject tmp = new JSONObject();
      tmp.put("uuid", uuid);
      tmp.put("ipv4", ipv4);
      tmp.put("ipv6", ipv6);
      tmp.put("adbPort", adbPort);
      return tmp;
    }

    public long getLastPostTime() {
      return lastPostTime;
    }
  }
}
