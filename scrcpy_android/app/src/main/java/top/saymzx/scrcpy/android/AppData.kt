package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import dev.mobile.dadb.AdbKeyPair
import kotlinx.coroutines.MainScope
import okhttp3.OkHttpClient
import java.io.File

@SuppressLint("Range")
class AppData : ViewModel() {

  // 是否初始化
  var isInit = false

  // 是否显示默认设备
  var isShowDefultDevice = false

  @SuppressLint("StaticFieldLeak")
  lateinit var main: MainActivity

  @SuppressLint("StaticFieldLeak")
  lateinit var fullScreenActivity: FullScreenActivity

  var fullScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

  // 全局协程域
  val mainScope = MainScope()

  // 公共工具库
  val publicTools = PublicTools()

  // 数据库管理
  lateinit var dbHelper: DbHelper

  // 设备列表管理
  lateinit var deviceListAdapter: DeviceListAdapter

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

  // OKHTTP
  var okhttpClient = OkHttpClient()

  // 当前版本号
  val versionCode = BuildConfig.VERSION_CODE

  // 初始化数据
  fun init() {
    isInit = true
    settings = main.getSharedPreferences("setting", Context.MODE_PRIVATE)
    // 获取系统分辨率
    val metric = DisplayMetrics()
    main.windowManager.defaultDisplay.getRealMetrics(metric)
    deviceWidth = metric.widthPixels
    deviceHeight = metric.heightPixels
    if (deviceWidth > deviceHeight) deviceWidth =
      deviceWidth xor deviceHeight xor deviceWidth.also { deviceHeight = it }
    // 数据库管理
    dbHelper = DbHelper(main, "scrcpy_android.db", 9)
    // 设备列表管理
    deviceListAdapter = DeviceListAdapter()
    // 从数据库获取设备列表
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
            cursor.getInt(cursor.getColumnIndex("setResolution")) == 1,
            cursor.getInt(cursor.getColumnIndex("defaultFull")) == 1
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
    // 密钥文件
    privateKey = File(main.applicationContext.filesDir, "private.key")
    publicKey = File(main.applicationContext.filesDir, "public.key")
    if (!privateKey.isFile || !publicKey.isFile) {
      AdbKeyPair.generate(privateKey, publicKey)
    }
    // 剪切板
    clipBorad = main.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

}