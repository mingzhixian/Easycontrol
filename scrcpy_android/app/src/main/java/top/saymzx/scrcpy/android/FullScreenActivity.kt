package top.saymzx.scrcpy.android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class FullScreenActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_full_screen)
    appData.fullScreenActivity = this
    findViewById<TextView>(R.id.full_screen_text).setOnClickListener {
      appData.isFocus = false
      for (i in appData.devices) {
        try {
          i.scrcpy?.stop("强行停止")
        } catch (_: Exception) {
        }
      }
      Toast.makeText(this, "已强制清理", Toast.LENGTH_SHORT).show()
      finish()
    }
    if (!appData.isFullScreenActivityInit) {
      try {
        requestedOrientation = intent.extras!!.getInt("isLandScape")
        appData.isFullScreenActivityInit = true
      } catch (_: Exception) {
      }
    }
  }

  override fun onResume() {
    // 全面屏
    appData.publicTools.setFullScreen(this)
    super.onResume()
  }

  // 如果处于专注模式则自动恢复界面
  override fun onPause() {
    if (appData.isFocus) {
      startActivity(intent)
    }
    super.onPause()
  }

}