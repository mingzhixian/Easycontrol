package top.saymzx.easycontrol.app.entity;

public class Device {
  public Integer id;
  public String name;
  public String address;

  public boolean isAudio;
  public Integer maxSize;
  public Integer maxFps;
  public Integer maxVideoBit;
  public boolean setResolution;

  public Device(Integer id,
                String name,
                String address,
                boolean isAudio,
                Integer maxSize,
                Integer maxFps,
                Integer maxVideoBit,
                boolean setResolution) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
  }

  public static Device getDefaultDevice() {
    return new Device(null, "", "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultVideoBit(), AppData.setting.getDefaultSetResolution());
  }
}
