package top.saymzx.scrcpy.android

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast

class FullScreenActivity : Activity() {
  private var isFocus = true
  var isChangeOrientation = false
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_full_screen)
    appData.fullScreenActivity = this
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
    updateOrientation()
  }

  override fun onResume() {
    // 全面屏
    appData.publicTools.setFullScreen(this)
    super.onResume()
  }

  // 如果有投屏处于全屏状态则自动恢复界面
  override fun onPause() {
    // 防止高版本安卓不断旋转
    if (!isChangeOrientation) {
      if (isFocus) {
        for (i in appData.devices) if (i.isFull && i.status >= 0) {
          startActivity(intent)
          break
        }
      }
    }
    isChangeOrientation = false
    super.onPause()
  }

  // 更新旋转方向
  fun updateOrientation(){
    if (resources.configuration.orientation != appData.fullScreenOrientation){
      isChangeOrientation = true
      requestedOrientation= appData.fullScreenOrientation
    }
  }
}