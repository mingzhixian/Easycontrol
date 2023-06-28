package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Intent.*
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.KeyEvent.*
import android.view.MotionEvent.*
import android.widget.*
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.*
import java.util.*

@SuppressLint("StaticFieldLeak")
lateinit var appData: AppData

class MainActivity : Activity(), ViewModelStoreOwner {

  companion object {
    var VIEWMODEL_STORE: ViewModelStore? = null
  }

  // 创建界面
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    appData = ViewModelProvider(this).get(AppData::class.java)
    if (!appData.isInit) appData.init(this)
    // 如果第一次使用展示介绍信息
    if (appData.settings.getBoolean("FirstUse", true)) startActivityForResult(
      Intent(
        this,
        ShowAppActivity::class.java
      ), 1
    )
    // 读取数据库并展示设备列表
    setDevicesList()
    // 添加按钮监听
    setAddDeviceListener()
    // 设置按钮监听
    setSetButtonListener()
    // 检查更新
    checkUpdate()
  }

  override fun onResume() {
    // 检查权限
    checkPermission()
    // 全面屏
    appData.publicTools.setFullScreen(this)
    super.onResume()
  }

  // 如果有投屏处于全屏状态则自动恢复界面
  override fun onPause() {
    super.onPause()
    for (i in appData.devices) if (i.isFull && i.status >= 0) {
      startActivity(intent)
      break
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    appData.mainScope.cancel()
  }

  // 其他页面回调
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // ShowApp页面回调
    if (requestCode == 1) {
      if (resultCode == 1) {
        appData.settings.edit().apply {
          putBoolean("FirstUse", false)
          apply()
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  // 检查权限
  private fun checkPermission() {
    // 检查悬浮窗权限
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      intent.data = Uri.parse("package:$packageName")
      startActivity(intent)
      Toast.makeText(appData.main, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
      return
    }
    // 检查通知权限
    if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
      // 请求通知权限
      val intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
        } else {
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .putExtra("app_package", packageName)
            .putExtra("app_uid", applicationInfo.uid)
        }
      startActivity(intent)
      Toast.makeText(appData.main, "请授予通知权限", Toast.LENGTH_SHORT).show()
      return
    }
  }

  // 读取数据库并展示设备列表
  private fun setDevicesList() {
    val devicesList = findViewById<ListView>(R.id.devices_list)
    devicesList.adapter = appData.deviceListAdapter
  }

  // 添加设备监听
  private fun setAddDeviceListener() {
    findViewById<TextView>(R.id.add_device).setOnLongClickListener {
      for (i in appData.devices) {
        try {
          i.scrcpy.stop("强行停止")
        } catch (_: Exception) {
        }
      }
      Toast.makeText(this, "已强制清理", Toast.LENGTH_SHORT).show()
      return@setOnLongClickListener true
    }
    findViewById<TextView>(R.id.add_device).setOnClickListener {
      // 显示添加界面
      val addDeviceView = LayoutInflater.from(this).inflate(R.layout.add_device, null, false)
      val builder: AlertDialog.Builder = AlertDialog.Builder(this)
      builder.setView(addDeviceView)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      // 设置默认值
      addDeviceView.findViewById<Spinner>(R.id.add_device_max_size).setSelection(
        appData.publicTools.getStringIndex(
          "1600",
          resources.getStringArray(R.array.maxSizeItems)
        )
      )
      addDeviceView.findViewById<Spinner>(R.id.add_device_fps).setSelection(
        appData.publicTools.getStringIndex(
          "60",
          resources.getStringArray(R.array.fpsItems)
        )
      )
      addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit).setSelection(
        appData.publicTools.getStringIndex(
          "8000000",
          resources.getStringArray(R.array.videoBitItems1)
        )
      )
      addDeviceView.findViewById<Spinner>(R.id.add_device_videoCodec).setSelection(
        appData.publicTools.getStringIndex(
          appData.settings.getString("setVideoCodec", "h264")!!,
          resources.getStringArray(R.array.videoCodecItems)
        )
      )
      addDeviceView.findViewById<Spinner>(R.id.add_device_audioCodec).setSelection(
        appData.publicTools.getStringIndex(
          appData.settings.getString("setAudioCodec", "opus")!!,
          resources.getStringArray(R.array.audioCodecItems)
        )
      )
      addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution).isChecked =
        appData.settings.getBoolean("setSetResolution", true)
      addDeviceView.findViewById<Switch>(R.id.add_device_default_full).isChecked =
        appData.settings.getBoolean("setDefaultFull", true)
      // 是否显示高级选项
      addDeviceView.findViewById<CheckBox>(R.id.add_device_is_options).setOnClickListener {
        addDeviceView.findViewById<LinearLayout>(R.id.add_device_options).visibility =
          if (addDeviceView.findViewById<CheckBox>(R.id.add_device_is_options).isChecked)
            View.VISIBLE
          else View.GONE
      }
      // 完成添加设备
      addDeviceView.findViewById<Button>(R.id.add_device_ok).setOnClickListener {
        // 名字不能为空
        if (addDeviceView.findViewById<EditText>(R.id.add_device_name).text.toString() != "") {
          appData.deviceListAdapter.newDevice(
            addDeviceView.findViewById<EditText>(R.id.add_device_name).text.toString(),
            addDeviceView.findViewById<EditText>(R.id.add_device_address).text.toString(),
            addDeviceView.findViewById<EditText>(R.id.add_device_port).text.toString().toInt(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_videoCodec).selectedItem.toString(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_audioCodec).selectedItem.toString(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_max_size).selectedItem.toString()
              .toInt(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_fps).selectedItem.toString()
              .toInt(),
            resources.getStringArray(R.array.videoBitItems1)[addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit).selectedItemPosition].toInt(),
            addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution).isChecked,
            addDeviceView.findViewById<Switch>(R.id.add_device_default_full).isChecked
          )
          dialog.cancel()
        }
      }
      dialog.show()
    }
  }

  // 设置按钮监听
  private fun setSetButtonListener() {
    findViewById<ImageView>(R.id.set).setOnClickListener {
      startActivity(Intent(this, SetActivity::class.java))
    }
  }

  // 检查更新
  private fun checkUpdate() {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
          .url("https://github.saymzx.top/api/repos/mingzhixian/scrcpy/releases/latest")
          .build()
        try {
          appData.okhttpClient.newCall(request).execute().use { response ->
            val json = JSONObject(response.body!!.string())
            val newVersionCode = json.getInt("tag_name")
            if (newVersionCode > appData.versionCode)
              withContext(Dispatchers.Main) {
                Toast.makeText(appData.main, "已发布新版本，可前往更新", Toast.LENGTH_LONG).show()
              }
          }
        } catch (_: Exception) {
        }
      }
    }
  }

  // 强制清理
  // ViewModel
  override fun getViewModelStore(): ViewModelStore {
    if (VIEWMODEL_STORE == null) {
      VIEWMODEL_STORE = ViewModelStore()
    }
    return VIEWMODEL_STORE!!
  }

}