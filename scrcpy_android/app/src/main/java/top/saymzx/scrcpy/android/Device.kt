package top.saymzx.scrcpy.android

class Device(
  val name: String,
  val address: String,
  val port: Int,
  val videoCodec: String,
  val audioCodec: String,
  val maxSize: Int,
  val fps: Int,
  val videoBit: Int,
  val setResolution: Boolean,
  val defaultFull: Boolean
) {
  var isFull = defaultFull

  // -1为停止状态，0为准备中，1为投屏中
  var status = -1
  lateinit var scrcpy: Scrcpy
}