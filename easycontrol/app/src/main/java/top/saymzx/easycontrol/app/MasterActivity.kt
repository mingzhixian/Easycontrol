package top.saymzx.easycontrol.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.easycontrol.app.databinding.ActivityMasterBinding
import top.saymzx.easycontrol.app.entity.Device
import top.saymzx.easycontrol.app.entity.FloatWindow
import top.saymzx.easycontrol.app.helper.DeviceListAdapter

class MasterActivity : Activity() {

  // 设备列表
  private lateinit var deviceListAdapter: DeviceListAdapter

  companion object {
    // 是否处于专注模式
    var isFocus = false

    // 需要启动默认设备
    var needStartDefault = true
  }

  // 创建界面
  private lateinit var masterActivity: ActivityMasterBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    masterActivity = ActivityMasterBinding.inflate(layoutInflater)
    setContentView(masterActivity.root)
    appData.main = this
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 设置设备列表适配器
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        deviceListAdapter =
          DeviceListAdapter(this@MasterActivity, appData.dbHelper.devices().getAll())
      }
      withContext(Dispatchers.Main) {
        masterActivity.masterDevicesList.adapter = deviceListAdapter
      }
    }
    // 添加按钮监听
    setAddDeviceListener()
    // 设置按钮监听
    setSetButtonListener()
    // 检查权限并启动默认设备
    if (checkPermission()) startDefault()
  }

  // 如果处于专注模式则自动恢复界面
  override fun onPause() {
    if (!isChangingConfigurations && isFocus) {
      startActivity(intent)
    }
    super.onPause()
  }

  // 启动默认设备
  private fun startDefault() {
    if (needStartDefault && appData.setting.defaultDevice != -1) {
      needStartDefault = false
      appData.mainScope.launch {
        withContext(Dispatchers.IO) {
          val devices = appData.dbHelper.devices().getById(appData.setting.defaultDevice)
          if (devices.isNotEmpty()) FloatWindow(devices[0])
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
      Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
      return false
    }
    return true
  }

  // 添加设备监听
  private fun setAddDeviceListener() {
    masterActivity.masterAdd.setOnClickListener {
      val dialog = appData.publicTools.createAddDeviceView(
        this,
        Device(
          null,
          "",
          "",
          5555,
          appData.setting.defaultVideoCodec,
          appData.setting.defaultAudioCodec,
          appData.setting.defaultMaxSize,
          appData.setting.defaultMaxFps,
          appData.setting.defaultVideoBit,
          appData.setting.defaultSetResolution
        ), deviceListAdapter
      )
      dialog.show()
    }
  }

  // 设置按钮监听
  private fun setSetButtonListener() {
    masterActivity.masterSet.setOnClickListener {
      startActivity(Intent(this, SetActivity::class.java))
    }
  }

}