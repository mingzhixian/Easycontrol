package top.saymzx.scrcpy.android

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class FullScreenActivity : Activity() {
  private var isFocus = true
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_full_screen)
    findViewById<TextView>(R.id.full_screen_text).setOnClickListener {
      isFocus = false
      for (i in appData.devices) {
        try {
          i.scrcpy?.stop("强行停止")
        } catch (_: Exception) {
        }
      }
      Toast.makeText(this, "已强制清理", Toast.LENGTH_SHORT).show()
      finish()
    }
  }

  override fun onResume() {
    // 全面屏
    appData.publicTools.setFullScreen(this)
    super.onResume()
  }

  // 如果有投屏处于全屏状态则自动恢复界面
  override fun onPause() {
    if (isFocus) {
      for (i in appData.devices) if (i.isFull && i.status >= 0) {
        val intent = Intent(this, FullScreenActivity::class.java)
        intent.flags = FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        break
      }
    }
    super.onPause()
  }
}