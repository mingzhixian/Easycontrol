package top.saymzx.scrcpy_android

class Device(
  val name: String,
  val address: String,
  val port: Int,
  val videoCodec: String,
  val maxSize: Int,
  val fps: Int,
  val videoBit: Int,
  val setResolution:Boolean
){
  var isFull=true
  var status=-1
}