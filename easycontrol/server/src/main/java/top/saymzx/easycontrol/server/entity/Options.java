/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Options {
  public static int serverPort=25166;
  public static boolean listenerClip=true;
  public static boolean isAudio = true;
  public static int maxSize = 1600;
  public static int maxVideoBit = 4000000;
  public static int maxFps = 60;
  public static boolean keepAwake = true;
  public static boolean supportH265 = true;
  public static boolean supportOpus = true;
  public static String startApp = "";

  public static void parse(String... args) {
    for (String arg : args) {
      int equalIndex = arg.indexOf('=');
      if (equalIndex == -1) throw new IllegalArgumentException("参数格式错误");
      String key = arg.substring(0, equalIndex);
      String value = arg.substring(equalIndex + 1);
      switch (key) {
        case "serverPort":
          serverPort = Integer.parseInt(value);
          break;
        case "listenerClip":
          listenerClip = Integer.parseInt(value) == 1;
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
        case "keepAwake":
          keepAwake = Integer.parseInt(value) == 1;
          break;
        case "supportH265":
          supportH265 = Integer.parseInt(value) == 1;
          break;
        case "supportOpus":
          supportOpus = Integer.parseInt(value) == 1;
          break;
        case "startApp":
          startApp = value;
          break;
      }
    }
  }
}

