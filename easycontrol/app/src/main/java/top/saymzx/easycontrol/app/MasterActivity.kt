package top.saymzx.easycontrol.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import top.saymzx.easycontrol.app.databinding.ActivityMasterBinding
import top.saymzx.easycontrol.app.entity.Device
import top.saymzx.easycontrol.app.entity.FloatWindow
import top.saymzx.easycontrol.app.helper.DeviceListAdapter


class MasterActivity : AppCompatActivity() {

  // 设备列表
  private val deviceListAdapter = DeviceListAdapter()

  // 创建界面
  private lateinit var masterActivity: ActivityMasterBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    masterActivity = ActivityMasterBinding.inflate(layoutInflater)
    setContentView(masterActivity.root)
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 检查权限
    checkPermission()
    // 设置设备列表适配器
    masterActivity.masterDevicesList.adapter = deviceListAdapter
    // 添加按钮监听
    setAddDeviceListener()
    // 设置按钮监听
    setSetButtonListener()
    // 启动默认设备
    startDefault()
  }


  // 启动默认设备
  private fun startDefault() {
    if (appData.setting.defaultDevice != -1) {
      val devices = appData.dbHelper.devices().getById(appData.setting.defaultDevice)
      if (devices.isNotEmpty()) FloatWindow(devices[0])
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

  // 添加设备监听
  private fun setAddDeviceListener() {
    masterActivity.masterAdd.setOnClickListener {
      val dialog = appData.publicTools.createAddDeviceView(
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