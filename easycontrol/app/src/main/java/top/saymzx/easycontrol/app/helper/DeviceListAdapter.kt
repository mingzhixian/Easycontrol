package top.saymzx.easycontrol.app.helper

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.easycontrol.app.SlaveActivity
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding
import top.saymzx.easycontrol.app.entity.Device
import top.saymzx.easycontrol.app.entity.FloatWindow
import java.net.Inet4Address


class DeviceListAdapter(private val context: Context, private var devices: List<Device>) :
  BaseAdapter() {
  override fun getCount(): Int = devices.size

  override fun getItem(p0: Int): Any {
    return devices[p0]
  }

  override fun getItemId(p0: Int): Long {
    return p0.toLong()
  }

  @SuppressLint("SetTextI18n")
  override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
    var convertView: View? = convertView
    if (convertView == null) {
      val devicesItemBinding = ItemDevicesItemBinding.inflate(LayoutInflater.from(context))
      convertView = devicesItemBinding.root
      convertView.tag = devicesItemBinding
    }
    val devicesItemBinding = convertView.tag as ItemDevicesItemBinding
    val device = devices[position]
    // 设置卡片值
    devicesItemBinding.deviceName.text = device.name
    devicesItemBinding.deviceAddress.text = "${device.address}:${device.port}"
    // 设置卡片首尾距离
    convertView.setPadding(
      0,
      if (position == 0) appData.publicTools.dp2px(40f) else 0,
      0,
      if (position == count - 1) appData.publicTools.dp2px(60f) else 0
    )
    // 单击事件
    devicesItemBinding.device.setOnClickListener {
      onClickCard(device)
    }
    // 长按事件
    devicesItemBinding.device.setOnLongClickListener {
      onLongClickCard(device)
      return@setOnLongClickListener true
    }
    return convertView
  }

  // 卡片单击事件
  private fun onClickCard(device: Device) {
    FloatWindow(device)
  }

  // 卡片长按事件
  private fun onLongClickCard(device: Device) {
    val itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context))
    val dialog = appData.publicTools.createDialog(context, itemSetDeviceBinding.root)
    // 设置界面按钮监听
    itemSetDeviceBinding.setDeviceDelete.setOnClickListener {
      dialog.hide()
      deleteDevice(device)
    }
    itemSetDeviceBinding.setDeviceChange.setOnClickListener {
      dialog.hide()
      val addDeviceDialog = appData.publicTools.createAddDeviceView(context, device, this)
      addDeviceDialog.show()
    }
    itemSetDeviceBinding.setDeviceDefult.setOnClickListener {
      dialog.hide()
      appData.setting.defaultDevice = device.id!!
    }
    itemSetDeviceBinding.setDeviceBack.setOnClickListener {
      dialog.hide()
      val ip: String
      try {
        // 获取IP地址
        ip = Inet4Address.getByName(device.address).hostAddress!!
      } catch (e: Exception) {
        Toast.makeText(context, "解析域名失败", Toast.LENGTH_LONG).show()
        return@setOnClickListener
      }
      SlaveActivity.back(ip, device.port)
    }
    dialog.show()
  }

  // 新建数据
  fun newDevice(device: Device) {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        appData.dbHelper.devices().insertAll(device)
        updateDevices()
      }
    }
  }

  // 更新数据
  fun updateDevice(device: Device) {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        appData.dbHelper.devices().updateUsers(device)
        updateDevices()
      }
    }
  }

  // 删除数据
  private fun deleteDevice(device: Device) {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        appData.dbHelper.devices().delete(device)
        updateDevices()
      }
    }
  }

  private suspend fun updateDevices() {
    devices = appData.dbHelper.devices().getAll()
    withContext(Dispatchers.Main) {
      notifyDataSetChanged()
    }
  }

}
