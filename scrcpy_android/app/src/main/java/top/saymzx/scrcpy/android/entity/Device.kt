package top.saymzx.scrcpy.android.entity

import org.json.JSONObject

var defaultVideoCodec = "h264"
var defaultAudioCodec = "opus"
var defaultMaxSize = 1920
var defaultFps = 60
var defaultVideoBit = 8000000
var defaultSetResolution = true
var defaultFull=true

class Device() {
  var name: String = ""
  var address: String = ""
  var port: Int = 0
  var videoCodec: String = defaultVideoCodec
  var audioCodec: String = defaultAudioCodec
  var maxSize: Int = defaultMaxSize
  var fps: Int = defaultFps
  var videoBit: Int = defaultVideoBit
  var setResolution: Boolean = defaultSetResolution

  var isFull = defaultFull
  var status = -1 // -1为停止状态，0为准备中，1为投屏中
  var scrcpy: Scrcpy? = null

  constructor (
    name: String,
    address: String,
    port: Int,
    videoCodec: String,
    audioCodec: String,
    maxSize: Int,
    fps: Int,
    videoBit: Int,
    setResolution: Boolean
  ) : this() {
    this.name = name
    this.address = address
    this.port = port
    this.videoCodec = videoCodec
    this.audioCodec = audioCodec
    this.maxSize = maxSize
    this.fps = fps
    this.videoBit = videoBit
    this.setResolution = setResolution
  }

  constructor(tmpJsonObject: JSONObject) : this() {
    this.name = tmpJsonObject.getString("name")
    this.address = tmpJsonObject.getString("address")
    this.port = tmpJsonObject.getInt("port")
    if (tmpJsonObject.has("videoCodec")) this.videoCodec = tmpJsonObject.getString("videoCodec")
    if (tmpJsonObject.has("audioCodec")) this.audioCodec = tmpJsonObject.getString("audioCodec")
    if (tmpJsonObject.has("maxSize")) this.maxSize = tmpJsonObject.getInt("maxSize")
    if (tmpJsonObject.has("fps")) this.fps = tmpJsonObject.getInt("fps")
    if (tmpJsonObject.has("videoBit")) this.videoBit = tmpJsonObject.getInt("videoBit")
    if (tmpJsonObject.has("setResolution")) this.setResolution =
      tmpJsonObject.getBoolean("setResolution")
  }

  // 序列化
  fun toJson(): JSONObject {
    val tmpJsonObject = JSONObject()
    tmpJsonObject.put("name", name)
    tmpJsonObject.put("address", address)
    tmpJsonObject.put("port", port)
    tmpJsonObject.put("videoCodec", videoCodec)
    tmpJsonObject.put("audioCodec", audioCodec)
    tmpJsonObject.put("maxSize", maxSize)
    tmpJsonObject.put("fps", fps)
    tmpJsonObject.put("videoBit", videoBit)
    tmpJsonObject.put("setResolution", setResolution)
    return tmpJsonObject
  }
}