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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import top.saymzx.scrcpy.android.databinding.ActivityMainBinding
import top.saymzx.scrcpy.android.databinding.AddDeviceBinding
import top.saymzx.scrcpy.android.entity.Scrcpy
import top.saymzx.scrcpy.android.entity.defaultAudioCodec
import top.saymzx.scrcpy.android.entity.defaultFps
import top.saymzx.scrcpy.android.entity.defaultFull
import top.saymzx.scrcpy.android.entity.defaultMaxSize
import top.saymzx.scrcpy.android.entity.defaultSetResolution
import top.saymzx.scrcpy.android.entity.defaultVideoBit
import top.saymzx.scrcpy.android.entity.defaultVideoCodec
import top.saymzx.scrcpy.android.helper.AppData
import java.io.*
import java.util.*

@SuppressLint("StaticFieldLeak")
lateinit var appData: AppData

class MainActivity : Activity(), ViewModelStoreOwner {

  companion object {
    var VIEWMODEL_STORE: ViewModelStore? = null
  }

  // 创建界面
  private lateinit var mainActivity: ActivityMainBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainActivity = ActivityMainBinding.inflate(layoutInflater)
    setContentView(mainActivity.root)
    // 初始化ViewModel
    appData = ViewModelProvider(this).get(AppData::class.java)
    appData.main = this
    if (!appData.isInit) appData.init()
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 如果第一次使用进入软件展示页
    if (appData.settings.getBoolean("FirstUse", true)) startActivityForResult(
      Intent(
        this, ShowAppActivity::class.java
      ), 1
    )
    // 设置设备列表适配器
    setDevicesList()
    // 添加按钮监听
    setAddDeviceListener()
    // 设置按钮监听
    setSetButtonListener()
    // 读取默认参数
    readDeviceDefault()
    // 检查更新
    checkUpdate()
  }

  override fun onResume() {
    // 检查权限
    if (checkPermission()) {
      // 仅在第一次启动默认设备
      if (!appData.isShowDefultDevice) {
        appData.isShowDefultDevice = true
        // 启动默认设备
        val defalueDevice = appData.settings.getString("DefaultDevice", "")
        if (defalueDevice != "") {
          for (i in appData.devices) {
            if (i.name == defalueDevice) {
              if (i.status == -1) {
                i.scrcpy = Scrcpy(i)
                i.scrcpy!!.start()
              }
              break
            }
          }
        }
      }
    }
    super.onResume()
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
  private fun checkPermission(): Boolean {
    // 检查悬浮窗权限
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      intent.data = Uri.parse("package:$packageName")
      startActivity(intent)
      Toast.makeText(appData.main, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
      return false
    }
    return true
  }

  // 设置设备列表适配器
  private fun setDevicesList() {
    val devicesList = mainActivity.devicesList
    devicesList.adapter = appData.deviceListAdapter
  }

  // 添加设备监听
  private fun setAddDeviceListener() {
    mainActivity.addDevice.setOnClickListener {
      // 显示添加界面
      val addDeviceView = AddDeviceBinding.inflate(layoutInflater)
      val builder: AlertDialog.Builder = AlertDialog.Builder(this)
      builder.setView(addDeviceView.root)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      // 设置默认值
      addDeviceView.addDeviceMaxSize.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.maxSizeItems))
      addDeviceView.addDeviceMaxSize.setSelection(
        appData.publicTools.getStringIndex(
          defaultMaxSize.toString(), resources.getStringArray(R.array.maxSizeItems)
        )
      )
      addDeviceView.addDeviceFps.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.fpsItems))
      addDeviceView.addDeviceFps.setSelection(
        appData.publicTools.getStringIndex(
          defaultFps.toString(), resources.getStringArray(R.array.fpsItems)
        )
      )
      addDeviceView.addDeviceVideoBit.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoBitItems2))
      addDeviceView.addDeviceVideoBit.setSelection(
        appData.publicTools.getStringIndex(
          defaultVideoBit.toString(), resources.getStringArray(R.array.videoBitItems1)
        )
      )
      addDeviceView.addDeviceVideoCodec.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoCodecItems))
      addDeviceView.addDeviceVideoCodec.setSelection(
        appData.publicTools.getStringIndex(
          defaultVideoCodec, resources.getStringArray(R.array.videoCodecItems)
        )
      )
      addDeviceView.addDeviceAudioCodec.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.audioCodecItems))
      addDeviceView.addDeviceAudioCodec.setSelection(
        appData.publicTools.getStringIndex(
          defaultAudioCodec, resources.getStringArray(R.array.audioCodecItems)
        )
      )
      addDeviceView.addDeviceSetResolution.isChecked =
        defaultSetResolution
      // 是否显示高级选项
      addDeviceView.addDeviceIsOptions.setOnClickListener {
        addDeviceView.addDeviceOptions.visibility =
          if (addDeviceView.addDeviceIsOptions.isChecked) View.VISIBLE
          else View.GONE
      }
      // 完成添加设备
      addDeviceView.addDeviceOk.setOnClickListener {
        val name = addDeviceView.addDeviceName.text.toString()
        val address = addDeviceView.addDeviceAddress.text.toString()
        // 名字不能为空
        if (name == "" || address == "") {
          Toast.makeText(this, "名字和地址不可为空", Toast.LENGTH_LONG).show()
          return@setOnClickListener
        }
        appData.deviceListAdapter.newDevice(
          name, address,
          addDeviceView.addDevicePort.text.toString().toInt(),
          addDeviceView.addDeviceVideoCodec.selectedItem.toString(),
          addDeviceView.addDeviceAudioCodec.selectedItem.toString(),
          addDeviceView.addDeviceMaxSize.selectedItem.toString()
            .toInt(),
          addDeviceView.addDeviceFps.selectedItem.toString()
            .toInt(),
          resources.getStringArray(R.array.videoBitItems1)[addDeviceView.addDeviceVideoBit.selectedItemPosition].toInt(),
          addDeviceView.addDeviceSetResolution.isChecked
        )
        dialog.cancel()
      }
      dialog.show()
    }
  }

  // 设置按钮监听
  private fun setSetButtonListener() {
    mainActivity.set.setOnClickListener {
      startActivity(Intent(this, SetActivity::class.java))
    }
  }

  // 读取默认参数
  private fun readDeviceDefault() {
    defaultVideoCodec = appData.settings.getString("defaultVideoCodec", "h264").toString()
    defaultAudioCodec = appData.settings.getString("defaultAudioCodec", "opus").toString()
    defaultMaxSize = appData.settings.getInt("defaultMaxSize", 1920)
    defaultFps = appData.settings.getInt("defaultFps", 60)
    defaultVideoBit = appData.settings.getInt("defaultVideoBit", 8000000)
    defaultSetResolution = appData.settings.getBoolean("defaultSetResolution", true)
    defaultFull = appData.settings.getBoolean("defaultFull", false)
  }

  // 检查更新
  private fun checkUpdate() {
    appData.netHelper.getJson("https://github.saymzx.top/api/repos/mingzhixian/scrcpy/releases/latest") {
      val newVersionCode = it?.getInt("tag_name")
      if (newVersionCode != null) {
        if (newVersionCode > appData.versionCode)
          Toast.makeText(this, "已发布新版本，可前往更新", Toast.LENGTH_LONG).show()
      }
    }
  }

  // ViewModel
  override fun getViewModelStore(): ViewModelStore {
    if (VIEWMODEL_STORE == null) {
      VIEWMODEL_STORE = ViewModelStore()
    }
    return VIEWMODEL_STORE!!
  }

}