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
    val address = "${device.address}:${device.port}"
    holder.textViewAddress.text = address
    // 单击打开投屏
    holder.linearLayout.setOnClickListener {
      Scrcpy(device, main).start()
    }
    // 长按删除
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
      deleteDeviceView.findViewById<Button>(R.id.delete_device_ok).setOnClickListener {
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
    }
    // 名称重复
    if (main.appData.dbHelper.writableDatabase.insert("DevicesDb", null, values).toInt() != -1) {
      main.appData.devices.add(Device(name, address, port, videoCodec, maxSize, fps, videoBit, setResolution))
      notifyDataSetChanged()
    }
  }
}