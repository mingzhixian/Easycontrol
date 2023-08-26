package top.saymzx.easycontrol.app.entity

import android.content.SharedPreferences

class Setting(private val sharedPreferences: SharedPreferences) {

  var appMode = -1
    get() {
      return sharedPreferences.getInt("appMode", -1)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("appMode", value)
        apply()
      }
    }

  /** 主控端 **/

  // 默认配置
  var defaultVideoCodec = "h264"
    get() {
      return sharedPreferences.getString("defaultVideoCodec", "h264")!!
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putString("defaultVideoCodec", value)
        apply()
      }
    }
  var defaultAudioCodec = "opus"
    get() {
      return sharedPreferences.getString("defaultAudioCodec", "opus")!!
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putString("defaultAudioCodec", value)
        apply()
      }
    }
  var defaultMaxSize = 1920
    get() {
      return sharedPreferences.getInt("defaultMaxSize", 1920)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("defaultMaxSize", value)
        apply()
      }
    }
  var defaultMaxFps = 60
    get() {
      return sharedPreferences.getInt("defaultFps", 60)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("defaultMaxFps", value)
        apply()
      }
    }
  var defaultVideoBit = 8
    get() {
      return sharedPreferences.getInt("defaultVideoBit", 8)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("defaultVideoBit", value)
        apply()
      }
    }
  var defaultSetResolution = false
    get() {
      return sharedPreferences.getBoolean("defaultSetResolution", false)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putBoolean("defaultSetResolution", value)
        apply()
      }
    }

  // 显示
  var slaveTurnOffScreen = true
    get() {
      return sharedPreferences.getBoolean("slaveTurnOffScreen", true)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putBoolean("slaveTurnOffScreen", value)
        apply()
      }
    }
  var defaultFull = false
    get() {
      return sharedPreferences.getBoolean("defaultFull", false)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putBoolean("defaultFull", value)
        apply()
      }
    }
  var floatBallSize = 55
    get() {
      return sharedPreferences.getInt("floatBallSize", 55)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("floatBallSize", value)
        apply()
      }
    }
  var showMoreInfo = false
    get() {
      return sharedPreferences.getBoolean("showMoreInfo", false)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putBoolean("showMoreInfo", value)
        apply()
      }
    }

  // 其他
  var defaultDevice = -1
    get() {
      return sharedPreferences.getInt("defaultDevice", -1)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("defaultDevice", value)
        apply()
      }
    }

  /** 被控端 **/

  var slaveAdbPort = 5555
    get() {
      return sharedPreferences.getInt("slaveAdbPort", 5555)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putInt("slaveAdbPort", value)
        apply()
      }
    }

  var slaveAutoBack = false
    get() {
      return sharedPreferences.getBoolean("slaveAutoBack", false)
    }
    set(value) {
      field = value
      sharedPreferences.edit().apply {
        putBoolean("slaveAutoBack", value)
        apply()
      }
    }

}