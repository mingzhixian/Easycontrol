package top.saymzx.easycontrol.app.entity;

import top.saymzx.easycontrol.app.helper.PublicTools;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;

  public final String uuid;
  public final int type;
  public String name;
  public String address;
  public boolean isAudio;
  public int maxSize;
  public int maxFps;
  public int maxVideoBit;
  public boolean setResolution;
  public boolean useH265;
  public int small_x;
  public static int SMALL_X = 200;
  public int small_y;
  public static int SMALL_Y = 200;
  public int small_length;
  public static int SMALL_LENGTH = 800;
  public int mini_y;
  public static int MINI_Y = 200;
  public boolean changeToFullOnConnect;

  public Device(String uuid,
                int type,
                String name,
                String address,
                boolean isAudio,
                int maxSize,
                int maxFps,
                int maxVideoBit,
                boolean setResolution,
                boolean useH265,
                int small_x,
                int small_y,
                int small_length,
                int mini_y) {
    this.uuid = uuid;
    this.type = type;
    this.name = name;
    this.address = address;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
    this.useH265 = useH265;
    this.small_x = small_x;
    this.small_y = small_y;
    this.small_length = small_length;
    this.mini_y = mini_y;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    if (SMALL_LENGTH == 0) SMALL_LENGTH = PublicTools.dp2px(500f);
    return new Device(uuid, type, uuid, "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getDefaultUseH265(), SMALL_X, SMALL_Y, SMALL_LENGTH, MINI_Y);
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

}
