package top.saymzx.scrcpy.android.helper

import android.app.AlertDialog
import android.content.ContentValues
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Toast
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData
import top.saymzx.scrcpy.android.databinding.AddDeviceBinding
import top.saymzx.scrcpy.android.databinding.DevicesItemBinding
import top.saymzx.scrcpy.android.databinding.SetDeviceBinding
import top.saymzx.scrcpy.android.entity.Device
import top.saymzx.scrcpy.android.entity.Scrcpy

class DeviceListAdapter : BaseAdapter() {
  override fun getCount(): Int = appData.devices.size

  override fun getItem(p0: Int): Any {
    return appData.devices[p0]
  }

  override fun getItemId(p0: Int): Long {
    return p0.toLong()
  }

  override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
    var convertView: View? = convertView
    if (convertView == null) {
      val devicesItemBinding = DevicesItemBinding.inflate(appData.main.layoutInflater)
      convertView = devicesItemBinding.root
      convertView.tag = devicesItemBinding
    }
    val devicesItemBinding = convertView.tag as DevicesItemBinding
    val device = appData.devices[position]
    devicesItemBinding.deviceName.text = device.name
    val addressID = "${device.address}:${device.port}"
    devicesItemBinding.deviceAddress.text = addressID
    val linearLayout = devicesItemBinding.device
    // 单击打开投屏
    linearLayout.setOnClickListener {
      if (device.status != -1) Toast.makeText(appData.main, "此设备正在投屏", Toast.LENGTH_SHORT)
        .show()
      else {
        device.scrcpy = Scrcpy(device)
        device.scrcpy!!.start()
      }
    }
    // 长按选项
    linearLayout.setOnLongClickListener {
      val setDeviceBinding = SetDeviceBinding.inflate(appData.main.layoutInflater)
      val builder: AlertDialog.Builder = AlertDialog.Builder(appData.main)
      builder.setView(setDeviceBinding.root)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      setDeviceBinding.setDeviceCancel.setOnClickListener {
        dialog.cancel()
      }
      // 修改
      setDeviceBinding.setDeviceChange.setOnClickListener {
        dialog.cancel()
        // 显示更新框
        val addDeviceBinding = AddDeviceBinding.inflate(appData.main.layoutInflater)
        val updateBuilder: AlertDialog.Builder = AlertDialog.Builder(appData.main)
        updateBuilder.setView(addDeviceBinding.root)
        updateBuilder.setCancelable(false)
        val updateDialog = updateBuilder.create()
        updateDialog.setCanceledOnTouchOutside(true)
        updateDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // 控件
        val name = addDeviceBinding.addDeviceName
        val address = addDeviceBinding.addDeviceAddress
        val port = addDeviceBinding.addDevicePort
        val videoCodec = addDeviceBinding.addDeviceVideoCodec
        val audioCodec = addDeviceBinding.addDeviceAudioCodec
        val maxSize = addDeviceBinding.addDeviceMaxSize
        val fps = addDeviceBinding.addDeviceFps
        val videoBit = addDeviceBinding.addDeviceVideoBit
        val setResolution = addDeviceBinding.addDeviceSetResolution
        // 是否显示高级选项
        addDeviceBinding.addDeviceIsOptions.setOnClickListener {
          addDeviceBinding.addDeviceOptions.visibility =
            if (addDeviceBinding.addDeviceIsOptions.isChecked)
              View.VISIBLE
            else View.GONE
        }
        // 确认按钮
        addDeviceBinding.addDeviceOk.setOnClickListener {
          updateDialog.cancel()
          val newAddress = address.text.toString()
          val newPort = port.text.toString().toInt()
          val newVideoCodec = videoCodec.selectedItem.toString()
          val newAudioCodec = audioCodec.selectedItem.toString()
          val newMaxSize = maxSize.selectedItem.toString().toInt()
          val newFps = fps.selectedItem.toString().toInt()
          val newVideoBit =
            appData.main.resources.getStringArray(R.array.videoBitItems1)[videoBit.selectedItemPosition].toInt()
          val newSetResolution = if (setResolution.isChecked) 1 else 0
          val values = ContentValues().apply {
            put("address", newAddress)
            put("port", newPort)
            put("videoCodec", newVideoCodec)
            put("audioCodec", newAudioCodec)
            put("maxSize", newMaxSize)
            put("fps", newFps)
            put("videoBit", newVideoBit)
            put("setResolution", newSetResolution)
          }
          if (appData.dbHelper.writableDatabase.update(
              "DevicesDb",
              values,
              "name=?",
              arrayOf(device.name)
            ) != -1
          ) {
            for (i in appData.devices.indices) {
              if (appData.devices[i].name == device.name) {
                appData.devices[i] = Device(
                  device.name,
                  newAddress,
                  newPort,
                  newVideoCodec,
                  newAudioCodec,
                  newMaxSize,
                  newFps,
                  newVideoBit,
                  newSetResolution == 1
                )
                break
              }
            }
            notifyDataSetChanged()
          }
        }
        updateDialog.show()
        // 填充参数
        name.setText(device.name)
        address.setText(device.address)
        port.setText(device.port.toString())
        videoCodec.adapter = ArrayAdapter(
          appData.main,
          R.layout.spinner_item,
          appData.main.resources.getStringArray(R.array.videoCodecItems)
        )
        videoCodec.setSelection(
          appData.publicTools.getStringIndex(
            device.videoCodec,
            appData.main.resources.getStringArray(R.array.videoCodecItems)
          )
        )
        audioCodec.adapter = ArrayAdapter(
          appData.main,
          R.layout.spinner_item,
          appData.main.resources.getStringArray(R.array.audioCodecItems)
        )
        audioCodec.setSelection(
          appData.publicTools.getStringIndex(
            device.audioCodec,
            appData.main.resources.getStringArray(R.array.audioCodecItems)
          )
        )
        maxSize.adapter = ArrayAdapter(
          appData.main,
          R.layout.spinner_item,
          appData.main.resources.getStringArray(R.array.maxSizeItems)
        )
        maxSize.setSelection(
          appData.publicTools.getStringIndex(
            device.maxSize.toString(),
            appData.main.resources.getStringArray(R.array.maxSizeItems)
          )
        )
        fps.adapter = ArrayAdapter(
          appData.main,
          R.layout.spinner_item,
          appData.main.resources.getStringArray(R.array.fpsItems)
        )
        fps.setSelection(
          appData.publicTools.getStringIndex(
            device.fps.toString(),
            appData.main.resources.getStringArray(R.array.fpsItems)
          )
        )
        videoBit.adapter = ArrayAdapter(
          appData.main,
          R.layout.spinner_item,
          appData.main.resources.getStringArray(R.array.videoBitItems2)
        )
        videoBit.setSelection(
          appData.publicTools.getStringIndex(
            device.videoBit.toString(),
            appData.main.resources.getStringArray(R.array.videoBitItems1)
          )
        )
        setResolution.isChecked = device.setResolution
        // 设置不可变参数
        name.isFocusable = false
        name.isFocusableInTouchMode = false
      }
      // 删除
      setDeviceBinding.setDeviceDelete.setOnClickListener {
        appData.dbHelper.writableDatabase.delete(
          "DevicesDb", "name = ?", arrayOf(device.name)
        )
        appData.devices.remove(device)
        notifyDataSetChanged()
        dialog.cancel()
      }
      // 设为默认
      setDeviceBinding.setDeviceDefult.setOnClickListener {
        appData.settings.edit().apply {
          putString("DefaultDevice", device.name)
          apply()
        }
        dialog.cancel()
      }
      dialog.show()
      return@setOnLongClickListener true
    }
    return convertView
  }

  //新建数据
  fun newDevice(
    name: String,
    address: String,
    port: Int,
    videoCodec: String,
    audioCodec: String,
    maxSize: Int,
    fps: Int,
    videoBit: Int,
    setResolution: Boolean
  ) {
    val values = ContentValues().apply {
      put("name", name)
      put("address", address)
      put("port", port)
      put("videoCodec", videoCodec)
      put("audioCodec", audioCodec)
      put("maxSize", maxSize)
      put("fps", fps)
      put("videoBit", videoBit)
      put("setResolution", if (setResolution) 1 else 0)
    }
    // 名称重复
    if (appData.dbHelper.writableDatabase.insert("DevicesDb", null, values).toInt() != -1) {
      appData.devices.add(
        Device(
          name,
          address,
          port,
          videoCodec,
          audioCodec,
          maxSize,
          fps,
          videoBit,
          setResolution
        )
      )
      notifyDataSetChanged()
    }
  }

  fun newDevice(device: Device) {
    newDevice(
      device.name,
      device.address,
      device.port,
      device.videoCodec,
      device.audioCodec,
      device.maxSize,
      device.fps,
      device.videoBit,
      device.setResolution
    )
  }
}
