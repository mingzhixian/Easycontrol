package top.saymzx.scrcpy.server.entity

object Options {
  var maxSize = 0
  var videoCodec = ""
  var audioCodec = ""
  var videoBitRate = 0
  var maxFps = 0

  fun parse(vararg args: String) {
    for (arg in args) {
      val equalIndex = arg.indexOf('=')
      require(equalIndex != -1) { "Invalid key=value pair: \"$arg\"" }
      val key = arg.substring(0, equalIndex)
      val value = arg.substring(equalIndex + 1)
      when (key) {
        "video_codec" -> videoCodec = value
        "audio_codec" -> audioCodec = value
        "max_size" -> maxSize = value.toInt() and 7.inv() // multiple of 8
        "video_bit_rate" -> videoBitRate = value.toInt()
        "max_fps" -> maxFps = value.toInt()
      }
    }
  }
}