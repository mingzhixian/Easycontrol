package top.saymzx.scrcpy.android.helper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData
import top.saymzx.scrcpy.android.databinding.LoadingBinding

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
    context.window.navigationBarColor = context.resources.getColor(R.color.background)
//    context.window.navigationBarDividerColor = context.resources.getColor(R.color.onBackground)
    // 状态栏
    context.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    context.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    context.window.statusBarColor = context.resources.getColor(R.color.background)
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
    val loadingBinding = LoadingBinding.inflate(LayoutInflater.from(context))
    loadingDialog.setView(loadingBinding.root)
    loadingBinding.loadingText.text = text
    if (isCanCancel) {
      loadingBinding.loadingCancel.visibility = View.VISIBLE
      loadingBinding.loadingCancel
        .setOnClickListener { cancelFun?.let { it1 -> it1() } }
    } else loadingBinding.loadingCancel.visibility = View.GONE
    loadingDialog.show()
    return loadingDialog
  }

  // DP转PX
  fun dp2px(dp: Float): Float = dp * appData.main.resources.displayMetrics.density
}