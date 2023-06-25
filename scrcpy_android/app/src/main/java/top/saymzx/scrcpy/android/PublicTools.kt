package top.saymzx.scrcpy.android

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager

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
}