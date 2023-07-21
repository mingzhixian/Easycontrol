package top.saymzx.scrcpy.android.entity

import org.json.JSONObject
import top.saymzx.scrcpy.android.appData

class SetValue {
  var appMode = 1

  // 主控端

  // 默认配置
  var defaultVideoCodec = "h264"
  var defaultAudioCodec = "opus"
  var defaultMaxSize = 1920
  var defaultFps = 60
  var defaultVideoBit = 8000000
  var defaultSetResolution = true

  // 显示
  var slaveTurnOffScreen = true
  var defaultFull = true
  var floatNavSize = 55
  var showFps = false

  // 其他
  var checkUpdate = true
  var defaultDevice = ""

  // 被控端

  var isSetSlaveAdbPort = false
  var slaveAdbPort = 5555

  // 读取设置值
  fun readSetValue() {
    appMode = appData.settings.getInt("appMode", 1)
    if (appMode == 1) {
      // 默认参数
      defaultVideoCodec = appData.settings.getString("defaultVideoCodec", "h264").toString()
      defaultAudioCodec = appData.settings.getString("defaultAudioCodec", "opus").toString()
      defaultMaxSize = appData.settings.getInt("defaultMaxSize", 1920)
      defaultFps = appData.settings.getInt("defaultFps", 60)
      defaultVideoBit = appData.settings.getInt("defaultVideoBit", 8000000)
      defaultSetResolution = appData.settings.getBoolean("defaultSetResolution", true)
      // 显示
      slaveTurnOffScreen = appData.settings.getBoolean("slaveTurnOffScreen", true)
      defaultFull = appData.settings.getBoolean("defaultFull", false)
      floatNavSize = appData.settings.getInt("floatNavSize", 55)
      showFps = appData.settings.getBoolean("showFps", false)
      // 其他
      checkUpdate = appData.settings.getBoolean("checkUpdate", true)
      defaultDevice = appData.settings.getString("defaultDevice", "").toString()
    } else {
      isSetSlaveAdbPort = appData.settings.getBoolean("isSetSlaveAdbPort", false)
      slaveAdbPort = appData.settings.getInt("slaveAdbPort", 5555)
    }
  }

  fun putAppMode(value: Int) {
    appMode = value
    appData.settings.edit().apply {
      putInt("appMode", value)
      apply()
    }
  }

  fun putDefaultVideoCodec(value: String) {
    defaultAudioCodec = value
    appData.settings.edit().apply {
      putString("defaultVideoCodec", value)
      apply()
    }
  }

  fun putDefaultAudioCodec(value: String) {
    defaultAudioCodec = value
    appData.settings.edit().apply {
      putString("defaultAudioCodec", value)
      apply()
    }
  }

  fun putDefaultMaxSize(value: Int) {
    defaultMaxSize = value
    appData.settings.edit().apply {
      putInt("defaultMaxSize", value)
      apply()
    }
  }

  fun putDefaultFps(value: Int) {
    defaultFps = value
    appData.settings.edit().apply {
      putInt("defaultFps", value)
      apply()
    }
  }

  fun putDefaultVideoBit(value: Int) {
    defaultVideoBit = value
    appData.settings.edit().apply {
      putInt("defaultVideoBit", value)
      apply()
    }
  }

  fun putDefaultSetResolution(value: Boolean) {
    defaultSetResolution = value
    appData.settings.edit().apply {
      putBoolean("defaultSetResolution", value)
      apply()
    }
  }

  fun putSlaveTurnOffScreen(value: Boolean) {
    slaveTurnOffScreen = value
    appData.settings.edit().apply {
      putBoolean("slaveTurnOffScreen", value)
      apply()
    }
  }

  fun putDefaultFull(value: Boolean) {
    defaultFull = value
    appData.settings.edit().apply {
      putBoolean("defaultFull", value)
      apply()
    }
  }

  fun putFloatNavSize(value: Int) {
    floatNavSize = value
    appData.settings.edit().apply {
      putInt("floatNavSize", value)
      apply()
    }
  }

  fun putShowFps(value: Boolean) {
    showFps = value
    appData.settings.edit().apply {
      putBoolean("showFps", value)
      apply()
    }
  }

  fun putDefaultDevice(value: String) {
    defaultDevice = value
    appData.settings.edit().apply {
      putString("defaultDevice", value)
      apply()
    }
  }

  fun putCheckUpdate(value: Boolean) {
    checkUpdate = value
    appData.settings.edit().apply {
      putBoolean("checkUpdate", value)
      apply()
    }
  }

  fun putIsSetSlaveAdbPort(value: Boolean) {
    isSetSlaveAdbPort = value
    appData.settings.edit().apply {
      putBoolean("isSetSlaveAdbPort", value)
      apply()
    }
  }

  fun putSlaveAdbPort(value: Int) {
    slaveAdbPort = value
    appData.settings.edit().apply {
      putInt("slaveAdbPort", value)
      apply()
    }
  }

  fun fromJson(jsonObject: JSONObject) {
    if (jsonObject.has("appMode")) putAppMode(jsonObject.getInt("appMode"))
    if (appMode == 1) {
      if (jsonObject.has("defaultVideoCodec")) putDefaultVideoCodec(jsonObject.getString("defaultVideoCodec"))
      if (jsonObject.has("defaultAudioCodec")) putDefaultAudioCodec(jsonObject.getString("defaultAudioCodec"))
      if (jsonObject.has("defaultMaxSize")) putDefaultMaxSize(jsonObject.getInt("defaultMaxSize"))
      if (jsonObject.has("defaultFps")) putDefaultFps(jsonObject.getInt("defaultFps"))
      if (jsonObject.has("defaultVideoBit")) putDefaultVideoBit(jsonObject.getInt("defaultVideoBit"))
      if (jsonObject.has("defaultSetResolution")) putDefaultSetResolution(jsonObject.getBoolean("defaultSetResolution"))
      if (jsonObject.has("slaveTurnOffScreen")) putSlaveTurnOffScreen(jsonObject.getBoolean("slaveTurnOffScreen"))
      if (jsonObject.has("defaultFull")) putDefaultFull(jsonObject.getBoolean("defaultFull"))
      if (jsonObject.has("floatNavSize")) putFloatNavSize(jsonObject.getInt("floatNavSize"))
      if (jsonObject.has("showFps")) putShowFps(jsonObject.getBoolean("showFps"))
      if (jsonObject.has("checkUpdate")) putCheckUpdate(jsonObject.getBoolean("checkUpdate"))
      if (jsonObject.has("defaultDevice")) putDefaultDevice(jsonObject.getString("defaultDevice"))
    } else {
      if (jsonObject.has("isSetSlaveAdbPort")) putIsSetSlaveAdbPort(jsonObject.getBoolean("isSetSlaveAdbPort"))
      if (jsonObject.has("slaveAdbPort")) putSlaveAdbPort(jsonObject.getInt("slaveAdbPort"))
    }

  }

  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put("appMode", appMode)
    if (appMode == 1) {
      json.put("defaultVideoCodec", defaultVideoCodec)
      json.put("defaultAudioCodec", defaultAudioCodec)
      json.put("defaultMaxSize", defaultMaxSize)
      json.put("defaultFps", defaultFps)
      json.put("defaultVideoBit", defaultVideoBit)
      json.put("defaultSetResolution", defaultSetResolution)
      json.put("slaveTurnOffScreen", slaveTurnOffScreen)
      json.put("defaultFull", defaultFull)
      json.put("floatNavSize", floatNavSize)
      json.put("showFps", showFps)
      json.put("checkUpdate", checkUpdate)
      json.put("defaultDevice", defaultDevice)
    } else {
      json.put("isSetSlaveAdbPort", isSetSlaveAdbPort)
      json.put("slaveAdbPort", slaveAdbPort)
    }
    return json
  }
}