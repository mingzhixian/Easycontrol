package top.saymzx.easycontrol.app.entity;

public class Device {
  public Integer id;
  public String name;
  public String address;
  public Integer maxSize;
  public Integer maxFps;
  public Integer maxVideoBit;
  public Boolean setResolution;

  public Device(Integer id,
                String name,
                String address,
                Integer maxSize,
                Integer maxFps,
                Integer maxVideoBit,
                Boolean setResolution) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
  }
}
