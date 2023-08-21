package top.saymzx.easycontrol.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import top.saymzx.easycontrol.app.databinding.ActivityFullScreenBinding


class FullScreenActivity : AppCompatActivity() {
  private lateinit var fullScreenActivity: ActivityFullScreenBinding
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
    val handler = Handler(this.mainLooper)
    val runnable: Runnable = object : Runnable {
      override fun run() {
        if (appData.isFocus) {
          if (requestedOrientation != appData.fullScreenOrientation)
            requestedOrientation = appData.fullScreenOrientation
        } else {
          finish()
        }
        handler.postDelayed(this, 200)
      }
    }
    handler.postDelayed(runnable, 200)
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