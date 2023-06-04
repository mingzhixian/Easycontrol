package top.saymzx.scrcpy_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView


class ShowApp : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_show_app)
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
    // 设置下一步按钮
    findViewById<Button>(R.id.show_app_agree).setOnClickListener {
      setResult(1)
      finish()
    }
  }
}