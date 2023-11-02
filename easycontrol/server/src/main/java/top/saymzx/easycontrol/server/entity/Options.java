/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Options {
  public static boolean isAudio;
  public static int maxSize;
  public static int videoBitRate;
  public static int maxFps;
  public static boolean turnOffScreen;
  public static boolean autoControlScreen;
  public static int setWidth;
  public static int setHeight;

  public static void parse(String... args) {
    for (String arg : args) {
      int equalIndex = arg.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalArgumentException("参数格式错误");
      }
      String key = arg.substring(0, equalIndex);
      String value = arg.substring(equalIndex + 1);
      switch (key) {
        case "is_audio":
          isAudio = Integer.parseInt(value) == 1;
          break;
        case "max_size":
          maxSize = Integer.parseInt(value) & ~7;
          break;
        case "max_fps":
          maxFps = Integer.parseInt(value);
          break;
        case "video_bit_rate":
          videoBitRate = Integer.parseInt(value) * 1000000;
          break;
        case "turn_off_screen":
          turnOffScreen = Integer.parseInt(value) == 1;
          break;
        case "auto_control_screen":
          autoControlScreen = Integer.parseInt(value) == 1;
          break;
        case "set_width":
          setWidth = Integer.parseInt(value);
          break;
        case "set_height":
          setHeight = Integer.parseInt(value);
          break;
      }
    }
  }
}

