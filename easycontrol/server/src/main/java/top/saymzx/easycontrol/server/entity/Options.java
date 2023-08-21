package top.saymzx.easycontrol.server.entity;

public final class Options {
  public static int maxSize;
  public static String videoCodec;
  public static String audioCodec;
  public static int videoBitRate;
  public static int maxFps;

  public static void parse(String... args) {
    for (int i = 1; i < args.length; ++i) {
      String arg = args[i];
      int equalIndex = arg.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalArgumentException("Invalid key=value pair: \"" + arg + "\"");
      }
      String key = arg.substring(0, equalIndex);
      String value = arg.substring(equalIndex + 1);
      switch (key) {
        case "video_bit_rate":
          videoBitRate = Integer.parseInt(value);
          break;
        case "max_size":
          maxSize = Integer.parseInt(value) & ~7;
          break;
        case "audio_codec":
          audioCodec = value;
          break;
        case "max_fps":
          maxFps = Integer.parseInt(value);
          break;
        case "video_codec":
          videoCodec = value;
      }
    }
  }
}

