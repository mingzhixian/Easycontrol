package top.saymzx.scrcpy.android.helper

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.MainScope
import top.saymzx.scrcpy.adb.AdbKeyPair
import top.saymzx.scrcpy.android.BuildConfig
import top.saymzx.scrcpy.android.MainActivity
import top.saymzx.scrcpy.android.entity.Device
import top.saymzx.scrcpy.android.entity.SetValue
import java.io.File

class AppData : ViewModel() {

  // 是否初始化
  var isInit = false

  @SuppressLint("StaticFieldLeak")
  lateinit var main: MainActivity

  // 是否处于专注模式
  var isFocus = false
  var fullScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

  // 全局协程域
  val mainScope = MainScope()

  // 公共工具库
  val publicTools = PublicTools()

  // 数据库工具
  lateinit var dbHelper: DbHelper

  // 网络工具
  val netHelper = NetHelper()

  // 设备列表管理
  val deviceListAdapter = DeviceListAdapter()

  // 设备列表
  val devices = ArrayList<Device>()

  // 密钥文件
  lateinit var privateKey: File
  lateinit var publicKey: File

  // 系统分辨率
  var deviceWidth = 0
  var deviceHeight = 0

  // 剪切板
  lateinit var clipBorad: ClipboardManager

  // 设置值
  lateinit var settings: SharedPreferences
  val setValue = SetValue()

  // 当前版本号
  val versionCode = BuildConfig.VERSION_CODE

  // 初始化数据
  fun init() {
    // 初始化标志位
    isInit = true
    // 设置键值对管理
    settings = main.getSharedPreferences("setting", Context.MODE_PRIVATE)
    // 读取设备分辨率
    readDeviceResolution()
    // 数据库管理
    dbHelper = DbHelper(main, "scrcpy_android.db", 9)
    // 读取数据库设备列表
    readDeviceList()
    // 读取密钥文件
    readKeyFiles()
    // 读取设置值
    setValue.readSetValue()
    // 剪切板管理
    clipBorad = main.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

  // 读取设备分辨率
  private fun readDeviceResolution() {
    val metric = DisplayMetrics()
    main.windowManager.defaultDisplay.getRealMetrics(metric)
    deviceWidth = metric.widthPixels
    deviceHeight = metric.heightPixels
    if (deviceWidth > deviceHeight) deviceWidth =
      deviceWidth xor deviceHeight xor deviceWidth.also { deviceHeight = it }
  }

  // 读取数据库设备列表
  @SuppressLint("Range")
  private fun readDeviceList() {
    val cursor = dbHelper.readableDatabase.query("DevicesDb", null, null, null, null, null, null)
    if (cursor.moveToFirst()) {
      do {
        devices.add(
          Device(
            cursor.getString(cursor.getColumnIndex("name")),
            cursor.getString(cursor.getColumnIndex("address")),
            cursor.getInt(cursor.getColumnIndex("port")),
            cursor.getString(cursor.getColumnIndex("videoCodec")),
            cursor.getString(cursor.getColumnIndex("audioCodec")),
            cursor.getInt(cursor.getColumnIndex("maxSize")),
            cursor.getInt(cursor.getColumnIndex("fps")),
            cursor.getInt(cursor.getColumnIndex("videoBit")),
            cursor.getInt(cursor.getColumnIndex("setResolution")) == 1
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
  }

  // 读取密钥文件
  private fun readKeyFiles() {
    privateKey = File(main.applicationContext.filesDir, "private.key")
    publicKey = File(main.applicationContext.filesDir, "public.key")
    if (!privateKey.isFile || !publicKey.isFile) {
      AdbKeyPair.generate(privateKey, publicKey)
    }
  }

}