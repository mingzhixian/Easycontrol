package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val main: MainActivity) :
  RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textViewName: TextView = view.findViewById(R.id.device_name)
    val textViewAddress: TextView = view.findViewById(R.id.device_address)
    val linearLayout: LinearLayout = view.findViewById(R.id.device)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_item, parent, false)
    return ViewHolder(view)
  }

  @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val device = main.appData.devices[position]
    holder.textViewName.text = device.name
    val addressID = "${device.address}:${device.port}"
    holder.textViewAddress.text = addressID
    // 单击打开投屏
    holder.linearLayout.setOnClickListener {
      if (device.status != -1) Toast.makeText(main, "此设备正在投屏", Toast.LENGTH_SHORT).show()
      else {
        device.scrcpy = Scrcpy(device, main)
        device.scrcpy.start()
      }
    }
    // 长按选项
    holder.linearLayout.setOnLongClickListener {
      val deleteDeviceView = LayoutInflater.from(main).inflate(R.layout.delete_device, null, false)
      val builder: AlertDialog.Builder = AlertDialog.Builder(main)
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
        val addDeviceView = LayoutInflater.from(main).inflate(R.layout.add_device, null, false)
        val updateBuilder: AlertDialog.Builder = AlertDialog.Builder(main)
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
        val maxSize = addDeviceView.findViewById<Spinner>(R.id.add_device_max_size)
        val fps = addDeviceView.findViewById<Spinner>(R.id.add_device_fps)
        val videoBit = addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit)
        val setResolution = addDeviceView.findViewById<Switch>(R.id.add_device_set_resolution)
        val defaultFull = addDeviceView.findViewById<Switch>(R.id.add_device_default_full)
        val floatNav = addDeviceView.findViewById<Switch>(R.id.add_device_float_nav)
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
          val newMaxSize = maxSize.selectedItem.toString().toInt()
          val newFps = fps.selectedItem.toString().toInt()
          val newVideoBit = videoBit.selectedItem.toString().toInt()
          val newSetResolution = if (setResolution.isChecked) 1 else 0
          val newDefaultFull = if (defaultFull.isChecked) 1 else 0
          val newFloatNav = if (floatNav.isChecked) 1 else 0
          val values = ContentValues().apply {
            put("address", newAddress)
            put("port", newPort)
            put("videoCodec", newVideoCodec)
            put("maxSize", newMaxSize)
            put("fps", newFps)
            put("videoBit", newVideoBit)
            put("setResolution", newSetResolution)
            put("defaultFull", newDefaultFull)
            put("floatNav", newFloatNav)
          }
          if (main.appData.dbHelper.writableDatabase.update(
              "DevicesDb",
              values,
              "name=?",
              arrayOf(device.name)
            ) != -1
          ) {
            main.appData.devices.remove(device)
            main.appData.devices.add(
              Device(
                device.name,
                newAddress,
                newPort,
                newVideoCodec,
                newMaxSize,
                newFps,
                newVideoBit,
                newSetResolution == 1,
                newDefaultFull == 1,
                newFloatNav == 1
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
          getStringIndex(
            device.videoCodec,
            main.resources.getStringArray(R.array.videoCodecItems)
          )
        )
        maxSize.setSelection(
          getStringIndex(
            device.maxSize.toString(),
            main.resources.getStringArray(R.array.maxSizeItems)
          )
        )
        fps.setSelection(
          getStringIndex(
            device.fps.toString(),
            main.resources.getStringArray(R.array.fpsItems)
          )
        )
        videoBit.setSelection(
          getStringIndex(
            device.videoBit.toString(),
            main.resources.getStringArray(R.array.videoBitItems)
          )
        )
        setResolution.isChecked = device.setResolution
        defaultFull.isChecked = device.defaultFull
        floatNav.isChecked = device.floatNav
        // 设置不可变参数
        name.isFocusable = false
        name.isFocusableInTouchMode = false
      }
      // 删除
      deleteDeviceView.findViewById<Button>(R.id.delete_device_delete).setOnClickListener {
        main.appData.dbHelper.writableDatabase.delete(
          "DevicesDb", "name = ?", arrayOf(device.name)
        )
        main.appData.devices.remove(device)
        notifyDataSetChanged()
        dialog.cancel()
      }
      dialog.show()
      return@setOnLongClickListener true
    }
  }

  override fun getItemCount() = main.appData.devices.size

  //新建数据
  @SuppressLint("NotifyDataSetChanged")
  fun newDevice(
    name: String,
    address: String,
    port: Int,
    videoCodec: String,
    maxSize: Int,
    fps: Int,
    videoBit: Int,
    setResolution: Boolean,
    defaultFull: Boolean,
    floatNav: Boolean
  ) {
    val values = ContentValues().apply {
      put("name", name)
      put("address", address)
      put("port", port)
      put("videoCodec", videoCodec)
      put("maxSize", maxSize)
      put("fps", fps)
      put("videoBit", videoBit)
      put("setResolution", if (setResolution) 1 else 0)
      put("defaultFull", if (defaultFull) 1 else 0)
      put("floatNav", if (floatNav) 1 else 0)
    }
    // 名称重复
    if (main.appData.dbHelper.writableDatabase.insert("DevicesDb", null, values).toInt() != -1) {
      main.appData.devices.add(
        Device(
          name,
          address,
          port,
          videoCodec,
          maxSize,
          fps,
          videoBit,
          setResolution,
          defaultFull,
          floatNav
        )
      )
      notifyDataSetChanged()
    }
  }

  // 获取string 在string array中的位置
  private fun getStringIndex(str: String, strArray: Array<String>): Int {
    for ((index, i) in strArray.withIndex()) {
      if (str == i) return index
      else index
    }
    // 找不到返回0
    return 0
  }

}