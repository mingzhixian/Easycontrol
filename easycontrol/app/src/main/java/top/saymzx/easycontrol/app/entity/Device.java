package top.saymzx.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NETWORK = 1;
  public static final int TYPE_LINK = 2;

  public final String uuid;
  public final int type;
  public String name;
  public String address = "";
  public boolean isAudio = true;
  public int maxSize = 1600;
  public int maxFps = 60;
  public int maxVideoBit = 4;
  public boolean useH265 = true;
  public boolean connectOnStart = false;
  public boolean customResolutionOnConnect = false;
  public boolean wakeOnConnect = true;
  public boolean lightOffOnConnect = false;
  public boolean showNavBarOnConnect = true;
  public boolean changeToFullOnConnect = false;
  public boolean keepWakeOnRunning = true;
  public boolean changeResolutionOnRunning = false;
  public boolean smallToMiniOnRunning = false;
  public boolean fullToMiniOnRunning = true;
  public boolean miniTimeoutOnRunning = false;
  public boolean lockOnClose = true;
  public boolean lightOnClose = false;
  public boolean reconnectOnClose = false;

  public int customResolutionWidth = 1080;
  public int customResolutionHeight = 2400;
  public int smallX = 200;
  public int smallY = 200;
  public int smallLength = 800;
  public int miniY = 200;

  public Device(String uuid, int type) {
    this.uuid = uuid;
    this.type = type;
    this.name = uuid;
  }

  public boolean isNetworkDevice() {
    return type == TYPE_NETWORK;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

}
