package top.saymzx.easycontrol.app.helper

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import top.saymzx.easycontrol.app.R
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.databinding.ItemAddDeviceBinding
import top.saymzx.easycontrol.app.databinding.ItemSpinnerBinding
import top.saymzx.easycontrol.app.databinding.ItemSwitchBinding
import top.saymzx.easycontrol.app.databinding.ItemTextBinding
import top.saymzx.easycontrol.app.databinding.ModuleDialogBinding
import top.saymzx.easycontrol.app.entity.Device
import kotlin.math.max

class PublicTools {

  // 设置全面屏
  fun setFullScreen(context: Activity) {
    // 全屏显示
    context.window.decorView.systemUiVisibility =
      (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      context.window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  // 设置状态栏导航栏颜色
  fun setStatusAndNavBar(context: Activity) {
    // 导航栏
    context.window.navigationBarColor = context.resources.getColor(R.color.background)
    // 状态栏
    context.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    context.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    context.window.statusBarColor = context.resources.getColor(R.color.background)
    context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
  }

  // DP转PX
  fun dp2px(dp: Float): Int = (dp * appData.main.resources.displayMetrics.density).toInt()

  // 获取当前界面宽高
  fun getScreenSize(context: Activity): Pair<Int, Int> {
    val metric = DisplayMetrics()
    context.windowManager.defaultDisplay.getRealMetrics(metric)
    return Pair(metric.widthPixels, metric.heightPixels)
  }

  // 创建弹窗
  fun createDialog(context: Context, view: View): Dialog {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setCancelable(false)
    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(true)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    ModuleDialogBinding.inflate(LayoutInflater.from(context)).dialogView.addView(view)
    return dialog
  }

  // 创建新建设备弹窗
  fun createAddDeviceView(
    context: Context,
    device: Device,
    deviceListAdapter: DeviceListAdapter
  ): Dialog {
    val itemAddDeviceBinding = ItemAddDeviceBinding.inflate(LayoutInflater.from(context))
    val dialog = createDialog(context, itemAddDeviceBinding.root)
    // 设置值
    itemAddDeviceBinding.addDeviceName.setText(device.name)
    itemAddDeviceBinding.addDeviceAddress.setText(device.address)
    itemAddDeviceBinding.addDevicePort.setText(device.port)
    // 创建View
    val videoCodec =
      createSpinnerCard(context, "视频编解码器", videoCodecAdapter, device.videoCodec)
    val audioCodec =
      createSpinnerCard(context, "音频编解码器", audioCodecAdapter, device.audioCodec)
    val maxSize = createSpinnerCard(context, "最大大小", maxSizeAdapter, device.maxSize.toString())
    val maxFps = createSpinnerCard(context, "最大帧率", maxFpsAdapter, device.maxFps.toString())
    val maxVideoBit =
      createSpinnerCard(context, "最大码率", videoBitAdapter, device.maxVideoBit.toString())
    val setResolution = createSwitchCard(context, "修改分辨率", device.setResolution)
    itemAddDeviceBinding.addDeviceOptions.addView(videoCodec.root)
    itemAddDeviceBinding.addDeviceOptions.addView(audioCodec.root)
    itemAddDeviceBinding.addDeviceOptions.addView(maxSize.root)
    itemAddDeviceBinding.addDeviceOptions.addView(maxFps.root)
    itemAddDeviceBinding.addDeviceOptions.addView(maxVideoBit.root)
    itemAddDeviceBinding.addDeviceOptions.addView(setResolution.root)
    // 是否显示高级选项
    itemAddDeviceBinding.addDeviceIsOptions.setOnClickListener {
      itemAddDeviceBinding.addDeviceOptions.visibility =
        if (itemAddDeviceBinding.addDeviceIsOptions.isChecked) View.VISIBLE
        else View.GONE
    }
    // 设置确认按钮监听
    itemAddDeviceBinding.addDeviceOk.setOnClickListener {
      val newDevice = Device(
        device.id,
        itemAddDeviceBinding.addDeviceName.text.toString(),
        itemAddDeviceBinding.addDeviceAddress.text.toString(),
        itemAddDeviceBinding.addDevicePort.text.toString().toInt(),
        videoCodec.itemSpinnerSpinner.toString(),
        audioCodec.itemSpinnerSpinner.selectedItem.toString(),
        maxSize.itemSpinnerSpinner.selectedItem.toString().toInt(),
        maxFps.itemSpinnerSpinner.selectedItem.toString().toInt(),
        maxVideoBit.itemSpinnerSpinner.selectedItem.toString()
          .toInt() * 1000000,
        setResolution.itemSwitchSwitch.isChecked
      )
      if (newDevice.id != null) deviceListAdapter.updateDevice(newDevice)
      else deviceListAdapter.newDevice(newDevice)
      dialog.hide()
    }
    return dialog
  }

  // 创建纯文本卡片
  fun createTextCard(
    context: Context,
    text: String,
    handle: (() -> Unit)? = null
  ): ItemTextBinding {
    val textView = ItemTextBinding.inflate(LayoutInflater.from(context))
    textView.root.text = text
    textView.root.setOnClickListener {
      handle?.let { it1 -> it1() }
    }
    return textView
  }

  // 创建开关卡片
  fun createSwitchCard(
    context: Context,
    text: String,
    isChecked: Boolean,
    handle: ((isChecked: Boolean) -> Unit)? = null
  ): ItemSwitchBinding {
    val switchView = ItemSwitchBinding.inflate(LayoutInflater.from(context))
    switchView.itemSwitchText.text = text
    switchView.itemSwitchSwitch.isChecked = isChecked
    switchView.itemSwitchSwitch.setOnCheckedChangeListener { _, checked ->
      handle?.let { it1 -> it1(checked) }
    }
    return switchView
  }

  // 创建列表卡片
  private val videoCodecList = arrayOf("h265", "h264")
  private val audioCodecList = arrayOf("opus", "aac")
  private val maxSizeList = arrayOf("2560", "1920", "1600", "1280", "1024", "800")
  private val maxFpsList = arrayOf("120", "90", "60", "30", "24", "10")
  private val videoBitList = arrayOf("20", "16", "12", "8", "4", "2", "1")
  val videoCodecAdapter =
    ArrayAdapter(appData.main, android.R.layout.simple_spinner_dropdown_item, videoCodecList)
  val audioCodecAdapter =
    ArrayAdapter(appData.main, android.R.layout.simple_spinner_dropdown_item, audioCodecList)
  val maxSizeAdapter =
    ArrayAdapter(appData.main, android.R.layout.simple_spinner_dropdown_item, maxSizeList)
  val maxFpsAdapter =
    ArrayAdapter(appData.main, android.R.layout.simple_spinner_dropdown_item, maxFpsList)
  val videoBitAdapter =
    ArrayAdapter(appData.main, android.R.layout.simple_spinner_dropdown_item, videoBitList)

  fun createSpinnerCard(
    context: Context,
    text: String,
    adapter: ArrayAdapter<String>,
    default: String,
    handle: ((select: String) -> Unit)? = null
  ): ItemSpinnerBinding {
    val spinnerView = ItemSpinnerBinding.inflate(LayoutInflater.from(context))
    spinnerView.itemSpinnerText.text = text
    spinnerView.itemSpinnerSpinner.adapter = adapter
    spinnerView.itemSpinnerSpinner.setSelection(adapter.getPosition(default))
    spinnerView.itemSpinnerSpinner.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
          handle?.let { it1 -> it1(spinnerView.itemSpinnerSpinner.selectedItem.toString()) }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}
      }
    return spinnerView
  }

}