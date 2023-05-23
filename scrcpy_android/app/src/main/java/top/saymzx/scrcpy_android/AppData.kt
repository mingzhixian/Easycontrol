package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("Range")
class AppData(): ViewModel() {

  // ADB客户端
  lateinit var adb: AndroidDebugBridgeClient
  // 数据库管理
  lateinit var dbHelper:DbHelper
  // 设备列表
  val devices = ArrayList<Device>()

  // 初始化数据
  fun init() {
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
            cursor.getInt(cursor.getColumnIndex("isFull")) == 1
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
  }
}