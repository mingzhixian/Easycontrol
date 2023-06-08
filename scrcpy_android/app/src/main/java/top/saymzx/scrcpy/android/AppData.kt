package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModel
import dadb.AdbKeyPair
import java.io.File

@SuppressLint("Range")
class AppData : ViewModel() {

  // 是否初始化
  var isInit = false

  // 数据库管理
  lateinit var dbHelper: DbHelper
  lateinit var deviceAdapter: DeviceAdapter

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

  // 加载框（全局通用）
  @SuppressLint("StaticFieldLeak")
  lateinit var loadingDialog: AlertDialog

  @SuppressLint("StaticFieldLeak")
  lateinit var loading: View

  // 初始化数据
  fun init(main: MainActivity) {
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
    dbHelper = DbHelper(main, "scrcpy_android.db", 7)
    deviceAdapter = DeviceAdapter(main)
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
            cursor.getInt(cursor.getColumnIndex("maxSize")),
            cursor.getInt(cursor.getColumnIndex("fps")),
            cursor.getInt(cursor.getColumnIndex("videoBit")),
            cursor.getInt(cursor.getColumnIndex("setResolution")) == 1,
            cursor.getInt(cursor.getColumnIndex("defaultFull")) == 1,
            cursor.getInt(cursor.getColumnIndex("floatNav")) == 1,
            cursor.getInt(cursor.getColumnIndex("setLoud")) == 1
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
    // 加载框
    val builder: AlertDialog.Builder = AlertDialog.Builder(main)
    builder.setCancelable(false)
    loadingDialog = builder.create()
    loadingDialog.setCanceledOnTouchOutside(false)
    loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    loading = LayoutInflater.from(main).inflate(R.layout.loading, null, false)
    loadingDialog.setView(loading)
    // 剪切板
    clipBorad = main.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

  // 显示加载框
  fun showLoading(text: String, isCanCancel: Boolean, cancelFun: (() -> Unit)?) {
    loading.findViewById<TextView>(R.id.loading_text).text = text
    if (isCanCancel) {
      loading.findViewById<Button>(R.id.loading_cancel).visibility = View.VISIBLE
      loading.findViewById<Button>(R.id.loading_cancel)
        .setOnClickListener { cancelFun?.let { it1 -> it1() } }
    } else loading.findViewById<Button>(R.id.loading_cancel).visibility = View.GONE
    loadingDialog.show()
  }

}