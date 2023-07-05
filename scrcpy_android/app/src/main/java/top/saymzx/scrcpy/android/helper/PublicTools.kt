package top.saymzx.scrcpy.android.helper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData

class PublicTools {

  // 获取string 在string array中的位置
  fun getStringIndex(str: String, strArray: Array<String>): Int {
    for ((index, i) in strArray.withIndex()) {
      if (str == i) return index
    }
    // 找不到返回0
    return 0
  }

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
    context.window.navigationBarColor = context.resources.getColor(R.color.appBackground)
//    context.window.navigationBarDividerColor = context.resources.getColor(R.color.onBackground)
    // 状态栏
    context.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    context.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    context.window.statusBarColor = context.resources.getColor(R.color.appBackground)
    context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
  }

  // 显示加载框
  fun showLoading(
    text: String,
    context: Context,
    isCanCancel: Boolean,
    cancelFun: (() -> Unit)?
  ): AlertDialog {
    // 加载框
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setCancelable(false)
    val loadingDialog = builder.create()
    loadingDialog.setCanceledOnTouchOutside(false)
    loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    val loading = LayoutInflater.from(context).inflate(R.layout.loading, null, false)
    loadingDialog.setView(loading)
    loading.findViewById<TextView>(R.id.loading_text).text = text
    if (isCanCancel) {
      loading.findViewById<Button>(R.id.loading_cancel).visibility = View.VISIBLE
      loading.findViewById<Button>(R.id.loading_cancel)
        .setOnClickListener { cancelFun?.let { it1 -> it1() } }
    } else loading.findViewById<Button>(R.id.loading_cancel).visibility = View.GONE
    loadingDialog.show()
    return loadingDialog
  }

  // DP转PX
  fun dp2px(dp: Float): Float = dp * appData.main.resources.displayMetrics.density
}