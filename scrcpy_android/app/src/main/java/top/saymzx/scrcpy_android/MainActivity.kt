package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Intent.*
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.KeyEvent.*
import android.view.MotionEvent.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.*
import java.util.*


class MainActivity : Activity(), ViewModelStoreOwner {

  companion object {
    var VIEWMODEL_STORE: ViewModelStore? = null
  }

  // 数据
  val appData = ViewModelProvider(this).get(AppData::class.java)

  // 广播处理
  private val scrcpyBroadcastReceiver = ScrcpyBroadcastReceiver()

  // 创建界面
  @SuppressLint("InflateParams")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    if (!appData.isInit) appData.init(this)
    // 检查悬浮窗权限
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      intent.data = Uri.parse("package:$packageName")
      startActivity(intent)
    }
    // 读取数据库并展示设备列表
    setDevicesList()
    // 设置添加按钮监听
    setAddDeviceListener()
    // 如果第一次使用展示介绍信息
    if (appData.settings.getBoolean("FirstUse", true)) startActivityForResult(
      Intent(
        this,
        ShowApp::class.java
      ), 1
    )
  }

  override fun onResume() {
    // 全面屏
    setFullScreen()
    super.onResume()
    // 注册广播用以关闭程序
    try {
      unregisterReceiver(scrcpyBroadcastReceiver)
    } catch (_: IllegalArgumentException) {
    }
    val filter = IntentFilter()
    filter.addAction(ACTION_SCREEN_OFF)
    filter.addAction("top.saymzx.scrcpy_android.notification")
    registerReceiver(scrcpyBroadcastReceiver, filter)
  }

  // 如果有投屏处于全屏状态则自动恢复界面
  override fun onPause() {
    super.onPause()
    for (i in appData.devices) if (i.isFull && i.status >= 0) {
      startActivity(intent)
      break
    }
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

  // 设置全面屏
  private fun setFullScreen() {
    // 全屏显示
    window.decorView.systemUiVisibility =
      (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  // 读取数据库并展示设备列表
  private fun setDevicesList() {
    val devices = findViewById<RecyclerView>(R.id.devices)
    devices.layoutManager = LinearLayoutManager(this)
    devices.adapter = appData.deviceAdapter
  }

  // 添加设备监听
  private fun setAddDeviceListener() {
    findViewById<TextView>(R.id.add_device).setOnClickListener {
      // 显示添加界面
      val addDeviceView = LayoutInflater.from(this).inflate(R.layout.add_device, null, false)
      val builder: AlertDialog.Builder = AlertDialog.Builder(this)
      builder.setView(addDeviceView)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
          appData.deviceAdapter.newDevice(
            addDeviceView.findViewById<EditText>(R.id.add_device_name).text.toString(),
            addDeviceView.findViewById<EditText>(R.id.add_device_address).text.toString(),
            addDeviceView.findViewById<EditText>(R.id.add_device_port).text.toString().toInt(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_videoCodec).selectedItem.toString(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_max_size).selectedItem.toString()
              .toInt(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_fps).selectedItem.toString()
              .toInt(),
            addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit).selectedItem.toString()
              .toInt(),
            addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution).isChecked,
            addDeviceView.findViewById<Switch>(R.id.add_device_default_full).isChecked,
            addDeviceView.findViewById<Switch>(R.id.add_device_float_nav).isChecked
          )
          dialog.cancel()
        }
      }
      dialog.show()
    }
  }

  // ViewModel
  override fun getViewModelStore(): ViewModelStore {
    if (VIEWMODEL_STORE == null) {
      VIEWMODEL_STORE = ViewModelStore()
    }
    return VIEWMODEL_STORE!!
  }

  // 广播处理
  inner class ScrcpyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      Log.i("Scrcpy", "收到广播，停止投屏")
      // 取消通知
      (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
      for (i in appData.devices) if (i.isFull && i.status >= 0) i.scrcpy.stop()
    }
  }

}