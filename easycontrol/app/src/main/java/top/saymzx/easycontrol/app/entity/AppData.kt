package top.saymzx.easycontrol.app.entity

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.MainScope
import top.saymzx.easycontrol.adb.AdbKeyPair
import top.saymzx.easycontrol.app.BuildConfig
import top.saymzx.easycontrol.app.helper.NetHelper
import top.saymzx.easycontrol.app.helper.PublicTools
import top.saymzx.easycontrol.app.helper.SQLDatabase
import java.io.File

class AppData(var main: Activity) {

  // 全局协程域
  val mainScope = MainScope()

  // 公共工具库
  val publicTools = PublicTools(main)

  // 数据库工具
  val dbHelper = Room.databaseBuilder(main, SQLDatabase::class.java, "app").build()

  // 网络工具
  val netHelper = NetHelper()

  // 密钥文件
  var keyPair: AdbKeyPair
  val privateKey = File(main.applicationContext.filesDir, "private.key")
  val publicKey = File(main.applicationContext.filesDir, "public.key")

  // 剪切板
  val clipBoard = main.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

  // 设置值
  var setting = Setting(main.getSharedPreferences("setting", Context.MODE_PRIVATE))

  // 当前版本号
  val versionCode = BuildConfig.VERSION_CODE

  init {
    // 读取密钥文件
    if (!privateKey.isFile || !publicKey.isFile) {
      AdbKeyPair.generate(privateKey, publicKey)
    }
    keyPair = AdbKeyPair.read(privateKey, publicKey)
  }

}