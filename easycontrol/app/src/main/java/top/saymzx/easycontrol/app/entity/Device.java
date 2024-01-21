package top.saymzx.easycontrol.app.entity;

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
  public boolean defaultFull;
  public boolean useH265;
  public boolean useOpus;
  public int small_p_p_x;
  public int small_p_p_y;
  public int small_p_p_width;
  public int small_p_p_height;
  public int small_p_l_x;
  public int small_p_l_y;
  public int small_p_l_width;
  public int small_p_l_height;
  public int small_l_p_x;
  public int small_l_p_y;
  public int small_l_p_width;
  public int small_l_p_height;
  public int small_l_l_x;
  public int small_l_l_y;
  public int small_l_l_width;
  public int small_l_l_height;
  public static int SMALL_X = 0;
  public static int SMALL_Y = 0;
  public static int SMALL_WIDTH = 0;
  public static int SMALL_HEIGHT = 0;
  public int mini_y;
  public static int MINI_Y =200;

  public Device(String uuid,
                int type,
                String name,
                String address,
                boolean isAudio,
                int maxSize,
                int maxFps,
                int maxVideoBit,
                boolean setResolution,
                boolean defaultFull,
                boolean useH265,
                boolean useOpus,
                int small_p_p_x, int small_p_p_y, int small_p_p_width, int small_p_p_height,
                int small_p_l_x, int small_p_l_y, int small_p_l_width, int small_p_l_height,
                int small_l_p_x, int small_l_p_y, int small_l_p_width, int small_l_p_height,
                int small_l_l_x, int small_l_l_y, int small_l_l_width, int small_l_l_height,
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
    this.defaultFull = defaultFull;
    this.useH265 = useH265;
    this.useOpus = useOpus;
    this.small_p_p_x = small_p_p_x;
    this.small_p_p_y = small_p_p_y;
    this.small_p_p_width = small_p_p_width;
    this.small_p_p_height = small_p_p_height;
    this.small_p_l_x = small_p_l_x;
    this.small_p_l_y = small_p_l_y;
    this.small_p_l_width = small_p_l_width;
    this.small_p_l_height = small_p_l_height;
    this.small_l_p_x = small_l_p_x;
    this.small_l_p_y = small_l_p_y;
    this.small_l_p_width = small_l_p_width;
    this.small_l_p_height = small_l_p_height;
    this.small_l_l_x = small_l_l_x;
    this.small_l_l_y = small_l_l_y;
    this.small_l_l_width = small_l_l_width;
    this.small_l_l_height = small_l_l_height;
    this.mini_y = mini_y;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    return new Device(uuid, type, uuid, "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getDefaultFull(), AppData.setting.getDefaultUseH265(), AppData.setting.getDefaultUseOpus(), SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, MINI_Y);
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

}
