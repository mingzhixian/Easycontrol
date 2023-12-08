package top.saymzx.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;
  public static final int TYPE_CENTER = 3;

  public final String uuid;
  public final Integer type;
  public String name;
  public String address;
  public boolean isAudio;
  public static final String isAudioDetail = "开启后将尝试传输音频，被控端需要安卓12之上";
  public Integer maxSize;
  public static final String maxSizeDetail = "画面大小限制，值越小分辨率越低，如设置为1600，则长和宽最大为1600";
  public Integer maxFps;
  public static final String maxFpsDetail = "最大帧率限制，值越低画面越卡顿，但流量也越小，延迟也会降低";
  public Integer maxVideoBit;
  public static final String maxVideoBitDetail = "码率越大视频损失越小体积越大，建议设置为4，过高会导致延迟增加";
  public boolean setResolution;
  public static final String setResolutionDetail = "开启后会自动修改被控端分辨率，可能会无法自动恢复(可手动恢复)，慎重考虑";
  public boolean turnOffScreen;
  public static final String turnOffScreenDetail = "开启后会在控制过程中保持被控端屏幕关闭";
  public boolean autoControlScreen;
  public static final String autoControlScreenDetail = "开启后会在连接时自动唤醒被控端，在连接中被控端待机后自动唤醒";
  public boolean defaultFull;
  public static final String defaultFullDetail = "开启后在连接成功后直接进入全屏状态";
  public boolean useH265;
  public static final String useH265Detail = "优先使用H265，视频体积小延迟低，实际以支持情况为主，若视频异常可尝试关闭";
  public boolean useOpus;
  public static final String useOpusDetail = "优先使用OPUS，音频体积小延迟低，实际以支持情况为主";
  public boolean useTunnel;
  public static final String useTunnelDetail = "开启后使用ADB隧道传输数据，否则使用单独端口(ADB端口加1)传输数据";

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
                boolean autoControlScreen,
                boolean defaultFull,
                boolean useH265,
                boolean useOpus,
                boolean useTunnel) {
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
    this.autoControlScreen = autoControlScreen;
    this.defaultFull = defaultFull;
    this.useH265 = useH265;
    this.useOpus=useOpus;
    this.useTunnel = useTunnel;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    return new Device(uuid, type, uuid, "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getTurnOffScreen(), AppData.setting.getAutoControlScreen(), AppData.setting.getDefaultFull(), AppData.setting.getUseH265(),AppData.setting.getUseOpus(), AppData.setting.getUseTunnel());
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isCenterDevice() {
    return type == TYPE_CENTER;
  }
}
