package top.saymzx.easycontrol.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import top.saymzx.easycontrol.adb.AdbKeyPair
import top.saymzx.easycontrol.app.databinding.ActivitySetBinding

class SetActivity : Activity() {
  private lateinit var setActivity: ActivitySetBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setActivity = ActivitySetBinding.inflate(layoutInflater)
    setContentView(setActivity.root)
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 设置页面
    drawUi()
    setBackButtonListener()
    setFloatNavSizeListener()
  }

  // 设置默认值
  @SuppressLint("SetTextI18n")
  private fun drawUi() {
    // 默认配置
    setActivity.setDefault.addView(
      appData.publicTools.createSpinnerCard(
        this,
        "视频编解码器",
        appData.publicTools.videoCodecAdapter,
        appData.setting.defaultVideoCodec
      ) { str -> appData.setting.defaultVideoCodec = str }.root
    )
    setActivity.setDefault.addView(
      appData.publicTools.createSpinnerCard(
        this,
        "音频编解码器",
        appData.publicTools.audioCodecAdapter,
        appData.setting.defaultAudioCodec
      ) { str -> appData.setting.defaultAudioCodec = str }.root
    )
    setActivity.setDefault.addView(
      appData.publicTools.createSpinnerCard(
        this,
        "最大大小",
        appData.publicTools.maxSizeAdapter,
        appData.setting.defaultMaxSize.toString()
      ) { str -> appData.setting.defaultMaxSize = str.toInt() }.root
    )
    setActivity.setDefault.addView(
      appData.publicTools.createSpinnerCard(
        this,
        "最大帧率",
        appData.publicTools.maxFpsAdapter,
        appData.setting.defaultMaxFps.toString()
      ) { str -> appData.setting.defaultMaxFps = str.toInt() }.root
    )
    setActivity.setDefault.addView(
      appData.publicTools.createSpinnerCard(
        this,
        "最大码率",
        appData.publicTools.videoBitAdapter,
        appData.setting.defaultVideoBit.toString()
      ) { str -> appData.setting.defaultVideoBit = str.toInt() }.root
    )
    setActivity.setDefault.addView(
      appData.publicTools.createSwitchCard(
        this,
        "修改分辨率",
        appData.setting.defaultSetResolution
      ) { isChecked ->
        appData.setting.defaultSetResolution = isChecked
      }.root
    )
    // 显示
    setActivity.setDisplay.addView(
      appData.publicTools.createSwitchCard(
        this,
        "被控端熄屏",
        appData.setting.slaveTurnOffScreen
      ) { isChecked ->
        appData.setting.slaveTurnOffScreen = isChecked
      }.root
    )
    setActivity.setDisplay.addView(
      appData.publicTools.createSwitchCard(
        this,
        "默认全屏启动",
        appData.setting.defaultFull
      ) { isChecked ->
        appData.setting.defaultFull = isChecked
      }.root
    )
    setActivity.setDisplay.addView(
      appData.publicTools.createSwitchCard(
        this,
        "显示更多信息",
        appData.setting.showMoreInfo
      ) { isChecked ->
        appData.setting.showMoreInfo = isChecked
      }.root
    )
    val progress = appData.setting.floatBallSize
    setActivity.setFloatNavSize.progress = progress - 35
    val preview = setActivity.setFloatNavPreview
    preview.layoutParams.apply {
      this.height = appData.publicTools.dp2px(progress.toFloat())
      preview.layoutParams = this
    }
    // 其他
    setActivity.setOther.addView(
      appData.publicTools.createTextCard(
        this,
        "清除默认设备"
      ) {
        appData.setting.defaultDevice = -1
        Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
      }.root
    )
    setActivity.setOther.addView(
      appData.publicTools.createTextCard(
        this,
        "重新生成密钥(需重新授权)"
      ) {
        AdbKeyPair.generate(appData.privateKey, appData.publicKey)
        appData.keyPair = AdbKeyPair.read(appData.privateKey, appData.publicKey)
        Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
      }.root
    )
    // 关于
    setActivity.setAbout.addView(
      appData.publicTools.createTextCard(
        this,
        "前往官网"
      ) {
        try {
          // 防止没有默认浏览器
          val intent = Intent(Intent.ACTION_VIEW)
          intent.addCategory(Intent.CATEGORY_BROWSABLE)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          intent.data = Uri.parse("https://mingzhixian.github.io/Easycontrol/")
          startActivity(intent)
        } catch (_: Exception) {
        }
      }.root
    )
    setActivity.setAbout.addView(
      appData.publicTools.createTextCard(
        this,
        "版本: ${BuildConfig.VERSION_NAME}"
      ) {
        appData.netHelper.getJson("https://github.com/api/repos/mingzhixian/Easycontrol/releases/latest") {
          val newVersionCode = it?.getInt("tag_name")
          if (newVersionCode != null) {
            if (newVersionCode > appData.versionCode)
              Toast.makeText(this, "已发布新版本，可前往更新", Toast.LENGTH_LONG).show()
            else
              Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_LONG).show()
          }
        }
      }.root
    )
  }

  // 设置返回按钮监听
  private fun setBackButtonListener() {
    setActivity.setBack.setOnClickListener {
      finish()
    }
  }

  // 设置悬浮球大小监听
  private fun setFloatNavSizeListener() {
    var progress = 0
    setActivity.setFloatNavSize.setOnSeekBarChangeListener(object :
      SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        progress = p1 + 35
        val preview = setActivity.setFloatNavPreview
        preview.layoutParams.apply {
          this.height = appData.publicTools.dp2px(progress.toFloat()).toInt()
          preview.layoutParams = this
        }
      }

      override fun onStartTrackingTouch(p0: SeekBar?) {
      }

      override fun onStopTrackingTouch(p0: SeekBar?) {
        appData.setting.floatBallSize = progress
      }

    })
  }

}