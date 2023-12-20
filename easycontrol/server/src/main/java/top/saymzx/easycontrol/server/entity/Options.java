/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Options {
  public static int tcpPort = 5556;
  public static boolean isAudio = true;
  public static int maxSize = 1600;
  public static int maxVideoBit = 8000000;
  public static int maxFps = 60;
  public static boolean turnOffScreen = true;
  public static boolean autoLockAfterControl = true;
  public static float reSize = -1;
  public static boolean useH265 = true;
  public static boolean useOpus = true;

  public static void parse(String... args) {
    for (String arg : args) {
      int equalIndex = arg.indexOf('=');
      if (equalIndex == -1) throw new IllegalArgumentException("参数格式错误");
      String key = arg.substring(0, equalIndex);
      String value = arg.substring(equalIndex + 1);
      switch (key) {
        case "tcpPort":
          tcpPort = Integer.parseInt(value);
          break;
        case "isAudio":
          isAudio = Integer.parseInt(value) == 1;
          break;
        case "maxSize":
          maxSize = Integer.parseInt(value);
          break;
        case "maxFps":
          maxFps = Integer.parseInt(value);
          break;
        case "maxVideoBit":
          maxVideoBit = Integer.parseInt(value) * 1000000;
          break;
        case "turnOffScreen":
          turnOffScreen = Integer.parseInt(value) == 1;
          break;
        case "autoLockAfterControl":
          autoLockAfterControl = Integer.parseInt(value) == 1;
          break;
        case "useH265":
          useH265 = Integer.parseInt(value) == 1;
          break;
        case "useOpus":
          useOpus = Integer.parseInt(value) == 1;
          break;
      }
    }
  }
}

