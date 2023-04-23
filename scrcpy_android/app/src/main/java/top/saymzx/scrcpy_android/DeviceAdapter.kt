package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.content.ContentValues
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val main: MainActivity) :
  RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
  private var dbHelper = DbHelper(main, "scrcpy_android.db", 1)
  var devices = initData()

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textViewName: TextView = view.findViewById(R.id.device_name)
    val textViewIP: TextView = view.findViewById(R.id.device_ip)
    val linearLayout: LinearLayout = view.findViewById(R.id.device)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_item, parent, false)
    return ViewHolder(view)
  }

  @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val device = devices[position]
    holder.textViewName.text = device.name
    holder.textViewIP.text = device.ip
    // 单击打开投屏
    holder.linearLayout.setOnClickListener {
      // 防止上次未关闭完全
      if (main.configs.status != -7) {
        Toast.makeText(main, "请等待上一个连接关闭完成", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }
      main.configs.remoteIp = device.ip
      main.configs.videoCodecMime = device.videoCodec
      main.configs.remoteHeight = device.resolution
      // 设置状态为准备中
      main.configs.status = 0
      main.startScrcpy()
    }
    // 长按删除
    holder.linearLayout.setOnLongClickListener {
      val deleteDeviceView =
        LayoutInflater.from(main).inflate(R.layout.delete_device, null, false)
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
        dbHelper.writableDatabase.delete(
          "DevicesDb",
          "name = ?",
          arrayOf(device.name)
        )
        devices.remove(device)
        notifyDataSetChanged()
        dialog.cancel()
      }
      dialog.show()
      return@setOnLongClickListener true
    }
  }

  override fun getItemCount() = devices.size

  // 初始化读取数据
  @SuppressLint("Range")
  private fun initData(): ArrayList<Device> {
    val devices = ArrayList<Device>()
    //数据库获取
    val cursor =
      dbHelper.readableDatabase.query("DevicesDb", null, null, null, null, null, null)
    if (cursor.moveToFirst()) {
      do {
        devices.add(
          Device(
            cursor.getString(cursor.getColumnIndex("name")),
            cursor.getString(cursor.getColumnIndex("ip")),
            cursor.getString(cursor.getColumnIndex("videoCodec")),
            cursor.getInt(cursor.getColumnIndex("resolution"))
          )
        )
      } while (cursor.moveToNext())
    }
    cursor.close()
    return devices
  }

  //新建数据
  @SuppressLint("NotifyDataSetChanged")
  fun newDevice(name: String, ip: String, videoCodec: String, resolution: Int) {
    val values = ContentValues().apply {
      put("name", name)
      put("ip", ip)
      put("videoCodec", videoCodec)
      put("resolution", resolution)
    }
    // ip重复
    if (dbHelper.writableDatabase.insert("DevicesDb", null, values).toInt() != -1) {
      devices.add(Device(name, ip, videoCodec, resolution))
      notifyDataSetChanged()
    }
  }
}