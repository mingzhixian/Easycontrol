/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Options {
  public static int maxSize;
  public static String videoCodec;
  public static String audioCodec;
  public static int videoBitRate;
  public static int maxFps;

  public static void parse(String... args) {
    for (String arg : args) {
      int equalIndex = arg.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalArgumentException("参数格式错误");
      }
      String key = arg.substring(0, equalIndex);
      String value = arg.substring(equalIndex + 1);
      switch (key) {
        case "video_codec":
          videoCodec = value;
          break;
        case "audio_codec":
          audioCodec = value;
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
      }
    }
  }
}

