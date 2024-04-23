package top.saymzx.easycontrol.app.entity;

import java.util.Objects;

public class Device {
  public static final int TYPE_NETWORK = 1;
  public static final int TYPE_LINK = 2;
  public final String uuid;
  public final int type;
  public String name;
  public String address = "";
  public String startApp = "";
  public int adbPort = 5555;
  public int serverPort = 25166;
  public boolean listenClip=true;
  public boolean isAudio = false;
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
  public int smallXLan = 200;
  public int smallYLan = 200;
  public int smallLengthLan = 800;
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

  public boolean isTempDevice() {
    return Objects.equals(name, "----");
  }

  public Device clone(String uuid) {
    Device newDevice = new Device(uuid, type);
    newDevice.name = name;
    newDevice.address = address;
    newDevice.startApp = startApp;
    newDevice.adbPort = adbPort;
    newDevice.serverPort = serverPort;
    newDevice.listenClip = listenClip;
    newDevice.isAudio = isAudio;
    newDevice.maxSize = maxSize;
    newDevice.maxFps = maxFps;
    newDevice.maxVideoBit = maxVideoBit;
    newDevice.useH265 = useH265;
    newDevice.connectOnStart = connectOnStart;
    newDevice.customResolutionOnConnect = customResolutionOnConnect;
    newDevice.wakeOnConnect = wakeOnConnect;
    newDevice.lightOffOnConnect = lightOffOnConnect;
    newDevice.showNavBarOnConnect = showNavBarOnConnect;
    newDevice.changeToFullOnConnect = changeToFullOnConnect;
    newDevice.keepWakeOnRunning = keepWakeOnRunning;
    newDevice.changeResolutionOnRunning = changeResolutionOnRunning;
    newDevice.smallToMiniOnRunning = smallToMiniOnRunning;
    newDevice.fullToMiniOnRunning = fullToMiniOnRunning;
    newDevice.miniTimeoutOnRunning = miniTimeoutOnRunning;
    newDevice.lockOnClose = lockOnClose;
    newDevice.lightOnClose = lightOnClose;
    newDevice.reconnectOnClose = reconnectOnClose;

    newDevice.customResolutionWidth = customResolutionWidth;
    newDevice.customResolutionHeight = customResolutionHeight;
    newDevice.smallX = smallX;
    newDevice.smallY = smallY;
    newDevice.smallLength = smallLength;
    newDevice.smallXLan = smallXLan;
    newDevice.smallYLan = smallYLan;
    newDevice.smallLengthLan = smallLengthLan;
    newDevice.miniY = miniY;
    return newDevice;
  }
}
