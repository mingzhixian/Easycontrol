package top.saymzx.scrcpy_android

import android.app.Activity
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView


class ShowApp : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_show_app)
      // 设置隐私政策链接
  }
}