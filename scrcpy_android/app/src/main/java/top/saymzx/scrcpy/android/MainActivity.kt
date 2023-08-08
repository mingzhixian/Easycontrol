package top.saymzx.scrcpy.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.scrcpy.adb.Adb
import top.saymzx.scrcpy.android.databinding.ActivityMainBinding
import top.saymzx.scrcpy.android.databinding.AddDeviceBinding
import top.saymzx.scrcpy.android.databinding.EditPortBinding
import top.saymzx.scrcpy.android.entity.Scrcpy
import top.saymzx.scrcpy.android.helper.AppData

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
    // 获取是否需要启动默认设备
    startDefault = intent.getBooleanExtra("startDefault", true)
    // 如果第一次使用进入软件展示页
    if (appData.settings.getBoolean("FirstUse", true)) startActivityForResult(
      Intent(
        this, ShowAppActivity::class.java
      ), 1
    )
    else readMode()
  }

  // 进入页面
  private var startDefault = true
  override fun onResume() {
    // 启动默认设备
    if (startDefault) {
      if (!appData.settings.getBoolean("FirstUse", true) && appData.setValue.appMode == 1) {
        startDefault()
      }
    } else {
      startDefault = true
    }
    super.onResume()
  }

  // 其他页面回调
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // ShowApp页面回调
    if (requestCode == 1) {
      readMode()
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  // 软件模式
  private fun readMode() {
    if (appData.setValue.appMode == 1) {
      asMaster()
    } else {
      asSlave()
    }
    // 检查更新
    if (appData.setValue.checkUpdate) appData.publicTools.checkUpdate(this, false)
  }

  // 作为控制端
  private fun asMaster() {
    // 检查权限
    checkPermission()
    // 设置设备列表适配器
    setDevicesList()
    // 添加按钮监听
    setAddDeviceListener()
    // 设置按钮监听
    setSetButtonListener()
  }

  // 启动默认设备
  private fun startDefault() {
    if (appData.setValue.defaultDevice != "") {
      for (i in appData.devices) {
        if (i.name == appData.setValue.defaultDevice) {
          if (i.status == -1) {
            i.scrcpy = Scrcpy(i)
            i.scrcpy!!.start()
          }
          break
        }
      }
    }
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
          appData.setValue.defaultMaxSize.toString(), resources.getStringArray(R.array.maxSizeItems)
        )
      )
      addDeviceView.addDeviceFps.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.fpsItems))
      addDeviceView.addDeviceFps.setSelection(
        appData.publicTools.getStringIndex(
          appData.setValue.defaultFps.toString(), resources.getStringArray(R.array.fpsItems)
        )
      )
      addDeviceView.addDeviceVideoBit.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoBitItems2))
      addDeviceView.addDeviceVideoBit.setSelection(
        appData.publicTools.getStringIndex(
          appData.setValue.defaultVideoBit.toString(),
          resources.getStringArray(R.array.videoBitItems1)
        )
      )
      addDeviceView.addDeviceVideoCodec.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoCodecItems))
      addDeviceView.addDeviceVideoCodec.setSelection(
        appData.publicTools.getStringIndex(
          appData.setValue.defaultVideoCodec, resources.getStringArray(R.array.videoCodecItems)
        )
      )
      addDeviceView.addDeviceAudioCodec.adapter =
        ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.audioCodecItems))
      addDeviceView.addDeviceAudioCodec.setSelection(
        appData.publicTools.getStringIndex(
          appData.setValue.defaultAudioCodec, resources.getStringArray(R.array.audioCodecItems)
        )
      )
      addDeviceView.addDeviceSetResolution.isChecked = appData.setValue.defaultSetResolution
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
          name,
          address,
          addDeviceView.addDevicePort.text.toString().toInt(),
          addDeviceView.addDeviceVideoCodec.selectedItem.toString(),
          addDeviceView.addDeviceAudioCodec.selectedItem.toString(),
          addDeviceView.addDeviceMaxSize.selectedItem.toString().toInt(),
          addDeviceView.addDeviceFps.selectedItem.toString().toInt(),
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

  // 作为被控端
  private fun asSlave() {
    // 防止多次调用
    if (mainActivity.addDevice.visibility == View.GONE) return
    // 取消画面
    mainActivity.addDevice.visibility = View.GONE
    mainActivity.set.visibility = View.GONE
    // 要求用户输入ADB端口
    if (!appData.setValue.isSetSlaveAdbPort) setSlaveAdbPort()
    else slaveBack()
  }

  // 弹窗输入adb端口
  private fun setSlaveAdbPort() {
    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    val editPortDialog = builder.create()
    editPortDialog.setCanceledOnTouchOutside(false)
    editPortDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    val editPortBinding = EditPortBinding.inflate(LayoutInflater.from(this))
    editPortDialog.setView(editPortBinding.root)
    // 设置监听
    editPortBinding.editPortOk.setOnClickListener {
      appData.setValue.putIsSetSlaveAdbPort(true)
      appData.setValue.putSlaveAdbPort(editPortBinding.editPortPort.text.toString().toInt())
      editPortDialog.cancel()
      slaveBack()
    }
    editPortDialog.show()
  }

  // 恢复
  private fun slaveBack() {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        try {
          val adb = Adb(
            "127.0.0.1", appData.setValue.slaveAdbPort, appData.keyPair
          )
          adb.runAdbCmd(
            "ps aux | grep scrcpy | grep -v grep | awk '{print \$2}' | xargs kill -9", false
          )
          adb.runAdbCmd("wm size reset", false)
          adb.close()
          withContext(Dispatchers.Main) {
            Toast.makeText(appData.main, "恢复程序执行完毕，将自动退出", Toast.LENGTH_LONG).show()
            delay(1000)
            finishAndRemoveTask()
            Runtime.getRuntime().exit(0)
          }
        } catch (_: Exception) {
          withContext(Dispatchers.Main) {
            Toast.makeText(appData.main, "连接失败", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  // ViewModel
  override val viewModelStore: ViewModelStore
    get() = if (VIEWMODEL_STORE == null) {
      VIEWMODEL_STORE = ViewModelStore()
      VIEWMODEL_STORE!!
    } else VIEWMODEL_STORE!!

}