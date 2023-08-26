package top.saymzx.easycontrol.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import top.saymzx.easycontrol.app.databinding.ActivityShowAppBinding


class ShowAppActivity : Activity() {
  private lateinit var showAppActivity: ActivityShowAppBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    showAppActivity = ActivityShowAppBinding.inflate(layoutInflater)
    setContentView(showAppActivity.root)
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 设置隐私用户政策
    setUserPriListener()
    // 设置模式按钮
    setModeListener()
  }

  // 设置隐私用户政策
  private fun setUserPriListener() {
    // 设置隐私政策链接
    showAppActivity.showAppPrivacy.setOnClickListener {
      try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://github.com/mingzhixian/Easycontrol/blob/master/PRIVACY.md")
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
        intent.data = Uri.parse("https://github.com/mingzhixian/Easycontrol/blob/master/HOW_TO_USE.md")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
  }

  // 设置同意按钮
  private fun setModeListener() {
    showAppActivity.showAppMaster.setOnClickListener {
      appData.setting.appMode = 1
      finish()
    }
    showAppActivity.showAppSlave.setOnClickListener {
      appData.setting.appMode = 2
      finish()
    }
  }

  // 禁止返回上一级
  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    Toast.makeText(this, "请选择工作模式", Toast.LENGTH_SHORT).show()
  }
}