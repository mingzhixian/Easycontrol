package top.saymzx.scrcpy.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import top.saymzx.scrcpy.android.databinding.ActivityShowAppBinding


class ShowAppActivity : Activity() {
  private lateinit var showAppActivity: ActivityShowAppBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    showAppActivity = ActivityShowAppBinding.inflate(layoutInflater)
    setContentView(showAppActivity.root)
    appData.publicTools.setStatusAndNavBar(this)
    // 设置隐私政策链接
    showAppActivity.showAppPrivacy.setOnClickListener {
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
    showAppActivity.showAppHowToUse.setOnClickListener {
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
    showAppActivity.showAppAgree.setOnClickListener {
      setResult(1)
      finish()
    }
  }
}