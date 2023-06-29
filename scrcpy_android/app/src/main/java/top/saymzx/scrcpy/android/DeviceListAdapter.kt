package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

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
      convertView = LayoutInflater.from(viewGroup!!.context).inflate(R.layout.devices_item, null)
    }
    val device = appData.devices[position]
    convertView!!.findViewById<TextView>(R.id.device_name).text = device.name
    val addressID = "${device.address}:${device.port}"
    convertView.findViewById<TextView>(R.id.device_address).text = addressID
    val linearLayout = convertView.findViewById<LinearLayout>(R.id.device)
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
      val deleteDeviceView =
        LayoutInflater.from(appData.main).inflate(R.layout.set_device, null, false)
      val builder: AlertDialog.Builder = AlertDialog.Builder(appData.main)
      builder.setView(deleteDeviceView)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      deleteDeviceView.findViewById<Button>(R.id.delete_device_cancel).setOnClickListener {
        dialog.cancel()
      }
      // 修改
      deleteDeviceView.findViewById<Button>(R.id.delete_device_change).setOnClickListener {
        dialog.cancel()
        // 显示更新框
        val addDeviceView =
          LayoutInflater.from(appData.main).inflate(R.layout.add_device, null, false)
        val updateBuilder: AlertDialog.Builder = AlertDialog.Builder(appData.main)
        updateBuilder.setView(addDeviceView)
        updateBuilder.setCancelable(false)
        val updateDialog = updateBuilder.create()
        updateDialog.setCanceledOnTouchOutside(true)
        updateDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // 控件
        val name = addDeviceView.findViewById<EditText>(R.id.add_device_name)
        val address = addDeviceView.findViewById<EditText>(R.id.add_device_address)
        val port = addDeviceView.findViewById<EditText>(R.id.add_device_port)
        val videoCodec = addDeviceView.findViewById<Spinner>(R.id.add_device_videoCodec)
        val audioCodec = addDeviceView.findViewById<Spinner>(R.id.add_device_audioCodec)
        val maxSize = addDeviceView.findViewById<Spinner>(R.id.add_device_max_size)
        val fps = addDeviceView.findViewById<Spinner>(R.id.add_device_fps)
        val videoBit = addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit)
        val setResolution = addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution)
        val defaultFull = addDeviceView.findViewById<Switch>(R.id.add_device_default_full)
        // 是否显示高级选项
        addDeviceView.findViewById<CheckBox>(R.id.add_device_is_options).setOnClickListener {
          addDeviceView.findViewById<LinearLayout>(R.id.add_device_options).visibility =
            if (addDeviceView.findViewById<CheckBox>(R.id.add_device_is_options).isChecked)
              View.VISIBLE
            else View.GONE
        }
        // 确认按钮
        addDeviceView.findViewById<Button>(R.id.add_device_ok).setOnClickListener {
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
          val newDefaultFull = if (defaultFull.isChecked) 1 else 0
          val values = ContentValues().apply {
            put("address", newAddress)
            put("port", newPort)
            put("videoCodec", newVideoCodec)
            put("audioCodec", newAudioCodec)
            put("maxSize", newMaxSize)
            put("fps", newFps)
            put("videoBit", newVideoBit)
            put("setResolution", newSetResolution)
            put("defaultFull", newDefaultFull)
          }
          if (appData.dbHelper.writableDatabase.update(
              "DevicesDb",
              values,
              "name=?",
              arrayOf(device.name)
            ) != -1
          ) {
            appData.devices.remove(device)
            appData.devices.add(
              Device(
                device.name,
                newAddress,
                newPort,
                newVideoCodec,
                newAudioCodec,
                newMaxSize,
                newFps,
                newVideoBit,
                newSetResolution == 1,
                newDefaultFull == 1
              )
            )
            notifyDataSetChanged()
          }
        }
        updateDialog.show()
        // 填充参数
        name.setText(device.name)
        address.setText(device.address)
        port.setText(device.port.toString())
        videoCodec.setSelection(
          appData.publicTools.getStringIndex(
            device.videoCodec,
            appData.main.resources.getStringArray(R.array.videoCodecItems)
          )
        )
        audioCodec.setSelection(
          appData.publicTools.getStringIndex(
            device.audioCodec,
            appData.main.resources.getStringArray(R.array.audioCodecItems)
          )
        )
        maxSize.setSelection(
          appData.publicTools.getStringIndex(
            device.maxSize.toString(),
            appData.main.resources.getStringArray(R.array.maxSizeItems)
          )
        )
        fps.setSelection(
          appData.publicTools.getStringIndex(
            device.fps.toString(),
            appData.main.resources.getStringArray(R.array.fpsItems)
          )
        )
        videoBit.setSelection(
          appData.publicTools.getStringIndex(
            device.videoBit.toString(),
            appData.main.resources.getStringArray(R.array.videoBitItems1)
          )
        )
        setResolution.isChecked = device.setResolution
        defaultFull.isChecked = device.defaultFull
        // 设置不可变参数
        name.isFocusable = false
        name.isFocusableInTouchMode = false
      }
      // 删除
      deleteDeviceView.findViewById<Button>(R.id.delete_device_delete).setOnClickListener {
        appData.dbHelper.writableDatabase.delete(
          "DevicesDb", "name = ?", arrayOf(device.name)
        )
        appData.devices.remove(device)
        notifyDataSetChanged()
        dialog.cancel()
      }
      // 设为默认
      deleteDeviceView.findViewById<Button>(R.id.delete_device_defult).setOnClickListener {
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
  @SuppressLint("NotifyDataSetChanged")
  fun newDevice(
    name: String,
    address: String,
    port: Int,
    videoCodec: String,
    audioCodec: String,
    maxSize: Int,
    fps: Int,
    videoBit: Int,
    setResolution: Boolean,
    defaultFull: Boolean
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
      put("defaultFull", if (defaultFull) 1 else 0)
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
          setResolution,
          defaultFull
        )
      )
      notifyDataSetChanged()
    }
  }
}
