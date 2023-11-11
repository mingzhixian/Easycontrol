package top.saymzx.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;
  public static final int TYPE_CENTER = 3;

  public final Integer id;
  public Integer type;
  public String name;
  public String address;
  public boolean isAudio;
  public Integer maxSize;
  public Integer maxFps;
  public Integer maxVideoBit;
  public boolean setResolution;
  public boolean turnOffScreen;
  public boolean autoControlScreen;
  public boolean defaultFull;

  public Device(Integer id,
                Integer type,
                String name,
                String address,
                boolean isAudio,
                Integer maxSize,
                Integer maxFps,
                Integer maxVideoBit,
                boolean setResolution,
                boolean turnOffScreen,
                boolean autoControlScreen,
                boolean defaultFull) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.address = address;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
    this.turnOffScreen = turnOffScreen;
    this.autoControlScreen = autoControlScreen;
    this.defaultFull = defaultFull;
  }

  public static Device getDefaultDevice() {
    return new Device(null, TYPE_NORMAL, "", "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getTurnOffScreen(), AppData.setting.getAutoControlScreen(), AppData.setting.getDefaultFull());
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

  public boolean isCenterDevice() {
    return type == TYPE_CENTER;
  }
}
