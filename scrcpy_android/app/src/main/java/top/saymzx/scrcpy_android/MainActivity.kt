package top.saymzx.scrcpy_android

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
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.*


class MainActivity : Activity(), ViewModelStoreOwner {

  companion object {
    var VIEWMODEL_STORE: ViewModelStore? = null
  }

  val appData = ViewModelProvider(this).get(AppData::class.java)

  // 创建界面
  @SuppressLint("InflateParams")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    appData.dbHelper=DbHelper(this, "scrcpy_android.db", 4)
    // 启动ADB客户端
    GlobalScope.launch {
      StartAdbInteractor().execute()
      appData.adb = AndroidDebugBridgeClientFactory().build()
    }
    appData.init()
    // 全屏显示
    setFullScreen()
    // 检查悬浮窗权限
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      intent.data = Uri.parse("package:$packageName")
      startActivity(intent)
    }
    val deviceAdapter = DeviceAdapter(this)
    // 读取数据库并展示设备列表
    setDevicesList(deviceAdapter)
    // 设置添加按钮监听
    setAddDeviceListener(deviceAdapter)
    // 保存文件到缓存
    saveFileToDir()
  }

  // 防止全屏状态失效
  override fun onResume() {
    setFullScreen()
    super.onResume()
  }

  // 设置全屏显示
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
  private fun setDevicesList(deviceAdapter: DeviceAdapter) {
    val devices = findViewById<RecyclerView>(R.id.devices)
    devices.layoutManager = LinearLayoutManager(this)
    devices.adapter = deviceAdapter
  }

  // 添加设备监听
  private fun setAddDeviceListener(deviceAdapter: DeviceAdapter) {
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
          deviceAdapter.newDevice(
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
            addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution).isChecked
          )
          dialog.cancel()
        }
      }
      dialog.show()
    }
  }

  // 保存文件到缓存
  private fun saveFileToDir() {
    // Server
    val serverFile =
      File(applicationContext.filesDir, "scrcpy_server${BuildConfig.VERSION_CODE}.jar")
    if (!serverFile.isFile) {
      Runtime.getRuntime().exec("rm ${applicationContext.filesDir}/scrcpy_server*")
      val server = resources.openRawResource(R.raw.scrcpy_server)
      val buffer = ByteArray(4096)
      var len = server.read(buffer)
      val stream = FileOutputStream(serverFile)
      do {
        stream.write(buffer, 0, len)
        len = server.read(buffer)
      } while (len > 0)
      stream.close()
    }
  }

  override fun getViewModelStore(): ViewModelStore {
    if (VIEWMODEL_STORE == null) {
      VIEWMODEL_STORE = ViewModelStore()
    }
    return VIEWMODEL_STORE!!
  }

}