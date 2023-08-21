package top.saymzx.easycontrol.app.helper

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import top.saymzx.easycontrol.app.MasterActivity
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding
import top.saymzx.easycontrol.app.entity.Device
import top.saymzx.easycontrol.app.entity.FloatWindow


class DeviceListAdapter : BaseAdapter() {
  private var devices = appData.dbHelper.devices().getAll()

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
      val devicesItemBinding = ItemDevicesItemBinding.inflate(appData.main.layoutInflater)
      convertView = devicesItemBinding.root
      convertView.tag = devicesItemBinding
    }
    val devicesItemBinding = convertView.tag as ItemDevicesItemBinding
    val device = devices[position]
    // 设置卡片值
    devicesItemBinding.deviceName.text = device.name
    devicesItemBinding.deviceAddress.text = "${device.address}:${device.port}"
    // 设置卡片首尾距离
    if (position == 0) {
      val lp = convertView.layoutParams as LinearLayout.LayoutParams
      lp.topMargin = appData.publicTools.dp2px(30f)
      convertView.layoutParams = lp
    } else if (position == count - 1) {
      val lp = convertView.layoutParams as LinearLayout.LayoutParams
      lp.bottomMargin = appData.publicTools.dp2px(50f)
      convertView.layoutParams = lp
    }
    // 单击事件
    convertView.setOnClickListener {
      onClickCard(device)
    }
    // 长按事件
    convertView.setOnLongClickListener {
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
    val itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(appData.main))
    val dialog = appData.publicTools.createDialog(appData.main, itemSetDeviceBinding.root)
    // 设置界面按钮监听
    itemSetDeviceBinding.setDeviceDelete.setOnClickListener {
      deleteDevice(device)
      dialog.hide()
    }
    itemSetDeviceBinding.setDeviceChange.setOnClickListener {
      val addDeviceDialog = appData.publicTools.createAddDeviceView(device, this)
      addDeviceDialog.show()
    }
    itemSetDeviceBinding.setDeviceDefult.setOnClickListener {
      appData.setting.defaultDevice = device.id!!
    }
    dialog.show()
  }

  // 新建数据
  fun newDevice(device: Device) {
    appData.dbHelper.devices().insertAll(device)
    devices = appData.dbHelper.devices().getAll()
    notifyDataSetChanged()
  }

  // 更新数据
  fun updateDevice(device: Device) {
    appData.dbHelper.devices().updateUsers(device)
    devices = appData.dbHelper.devices().getAll()
    notifyDataSetChanged()
  }

  // 删除数据
  fun deleteDevice(device: Device) {
    appData.dbHelper.devices().delete(device)
    devices = appData.dbHelper.devices().getAll()
    notifyDataSetChanged()
  }

}
