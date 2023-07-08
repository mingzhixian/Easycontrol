package top.saymzx.scrcpy.android

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.saymzx.scrcpy.android.databinding.ActivityFullScreenBinding

class FullScreenActivity : Activity() {
  private lateinit var fullScreenActivity: ActivityFullScreenBinding
  private val fullScreenScope = MainScope()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    fullScreenActivity = ActivityFullScreenBinding.inflate(layoutInflater)
    setContentView(fullScreenActivity.root)
    fullScreenActivity.fullScreenText.setOnClickListener {
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
    fullScreenScope.launch {
      while (appData.isFocus) {
        if (requestedOrientation != appData.fullScreenOrientation)
          requestedOrientation = appData.fullScreenOrientation
        delay(200)
      }
      finish()
    }
  }

  override fun onResume() {
    // 全面屏
    appData.publicTools.setFullScreen(this)
    super.onResume()
  }

  override fun onDestroy() {
    fullScreenScope.cancel()
    super.onDestroy()
  }

  // 如果处于专注模式则自动恢复界面
  override fun onPause() {
    if (appData.isFocus) {
      startActivity(intent)
    }
    super.onPause()
  }

}