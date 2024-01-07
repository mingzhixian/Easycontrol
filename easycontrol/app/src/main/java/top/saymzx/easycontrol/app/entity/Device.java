package top.saymzx.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;
  public static final int TYPE_CLOUD = 3;

  public final String uuid;
  public final Integer type;
  public String name;
  public String address;
  public boolean isAudio;
  public Integer maxSize;
  public Integer maxFps;
  public Integer maxVideoBit;
  public boolean setResolution;
  public boolean turnOffScreen;
  public boolean autoLockAfterControl;
  public boolean defaultFull;
  public boolean useH265;
  public boolean useOpus;
  public boolean useTunnel;
  public Integer window_x;
  public Integer window_y;
  public Integer window_width;
  public Integer window_height;

  public Device(String uuid,
                Integer type,
                String name,
                String address,
                boolean isAudio,
                Integer maxSize,
                Integer maxFps,
                Integer maxVideoBit,
                boolean setResolution,
                boolean turnOffScreen,
                boolean autoLockAfterControl,
                boolean defaultFull,
                boolean useH265,
                boolean useOpus,
                boolean useTunnel,
                Integer window_x,
                Integer window_y,
                Integer window_width,
                Integer window_height) {
    this.uuid = uuid;
    this.type = type;
    this.name = name;
    this.address = address;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
    this.turnOffScreen = turnOffScreen;
    this.autoLockAfterControl = autoLockAfterControl;
    this.defaultFull = defaultFull;
    this.useH265 = useH265;
    this.useOpus = useOpus;
    this.useTunnel = useTunnel;
    this.window_x = window_x;
    this.window_y = window_y;
    this.window_width = window_width;
    this.window_height = window_height;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    return new Device(uuid, type, uuid, "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getDefaultTurnOffScreen(), AppData.setting.getDefaultAutoLockAfterControl(), AppData.setting.getDefaultFull(), AppData.setting.getDefaultUseH265(), AppData.setting.getDefaultUseOpus(), AppData.setting.getDefaultUseTunnel(), 0, 0, 0, 0);
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isCloudDevice() {
    return type == TYPE_CLOUD;
  }
}
