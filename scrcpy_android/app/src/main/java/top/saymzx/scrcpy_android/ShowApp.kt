package top.saymzx.scrcpy_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView


class ShowApp : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_show_app)
    // 全屏显示
    window.decorView.systemUiVisibility =
      (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    // 设置隐私政策链接
    findViewById<TextView>(R.id.show_app_privacy).setOnClickListener {
      try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://github.com/mingzhixian/scrcpy/blob/master/PRIVACY.md")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
    // 设置使用说明链接
    findViewById<TextView>(R.id.show_app_how_to_use).setOnClickListener {
      try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://github.com/mingzhixian/scrcpy/blob/master/HOW_TO_USE.md")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
    // 设置下一步按钮
    findViewById<Button>(R.id.show_app_agree).setOnClickListener {
      setResult(1)
      finish()
    }
  }
}