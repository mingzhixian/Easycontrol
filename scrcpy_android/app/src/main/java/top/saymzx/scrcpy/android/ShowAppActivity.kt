package top.saymzx.scrcpy.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import top.saymzx.scrcpy.android.databinding.ActivityShowAppBinding
import top.saymzx.scrcpy.android.databinding.ModeSelectBinding


class ShowAppActivity : Activity() {
  private lateinit var showAppActivity: ActivityShowAppBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    showAppActivity = ActivityShowAppBinding.inflate(layoutInflater)
    setContentView(showAppActivity.root)
    appData.publicTools.setStatusAndNavBar(this)
    // 设置隐私用户政策
    setUserPriListener()
    // 设置同意按钮
    setAgreeListener()
  }

  // 设置隐私用户政策
  private fun setUserPriListener() {
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
  }

  // 设置同意按钮
  private lateinit var modeSelectDialog: AlertDialog
  private fun setAgreeListener() {
    showAppActivity.showAppAgree.setOnClickListener {
      // 弹窗选择模式
      val builder: AlertDialog.Builder = AlertDialog.Builder(this)
      builder.setCancelable(false)
      modeSelectDialog = builder.create()
      modeSelectDialog.setCanceledOnTouchOutside(false)
      modeSelectDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      val modeSelectBinding = ModeSelectBinding.inflate(LayoutInflater.from(this))
      modeSelectDialog.setView(modeSelectBinding.root)
      modeSelectBinding.modeSelectMaster.setOnClickListener {
        saveSet(1)
      }
      modeSelectBinding.modeSelectSlave.setOnClickListener {
        saveSet(0)
      }
      modeSelectDialog.show()
    }
  }

  // 保存设置
  private fun saveSet(mode: Int) {
    appData.settings.edit().apply {
      putBoolean("FirstUse", false)
      apply()
    }
    appData.setValue.putAppMode(mode)
    modeSelectDialog.cancel()
    finish()
  }

  // 禁止返回上一级
  override fun onBackPressed() {
    Toast.makeText(this, "请先同意用户及隐私协议", Toast.LENGTH_SHORT).show()
  }
}