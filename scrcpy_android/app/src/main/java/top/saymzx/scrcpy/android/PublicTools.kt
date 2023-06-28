package top.saymzx.scrcpy.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

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
    // 隐藏标题栏
    context.actionBar?.hide()
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