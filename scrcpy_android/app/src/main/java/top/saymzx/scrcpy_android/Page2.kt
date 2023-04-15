package top.saymzx.scrcpy_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Page2 : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_page2)
  }

  // 自动恢复界面
  override fun onPause() {
    super.onPause()
    val intent = Intent(this, Page2::class.java)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.flags =
      Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    startActivity(intent)
  }
}