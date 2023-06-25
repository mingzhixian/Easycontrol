package top.saymzx.scrcpy.android

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SetActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_set)
    // 全面屏
    appData.publicTools.setFullScreen(this)
    // 设置默认值
    setValue()
    // 设置返回按钮监听
    setBackButtonListener()
    // 设置视频编解码器监听
    setVideoCodecListener()
    // 设置音频编解码器监听
    setAudioCodecListener()
    // 设置是否修改分辨率监听
    setSetResolutionListener()
    // 设置是否修改分辨率监听
    setDefaultFullListener()
    // 设置是否修改分辨率监听
    setFloatNavListener()
    // 设置导出按钮监听
    setExportListener()
    // 设置导入按钮监听
    setImportListener()
  }

  // 设置默认值
  private fun setValue() {
    findViewById<Spinner>(R.id.set_videoCodec).setSelection(
      appData.publicTools.getStringIndex(
        appData.settings.getString("setVideoCodec", "h264")!!,
        resources.getStringArray(R.array.videoCodecItems)
      )
    )
    findViewById<Spinner>(R.id.set_audioCodec).setSelection(
      appData.publicTools.getStringIndex(
        appData.settings.getString("setAudioCodec", "opus")!!,
        resources.getStringArray(R.array.audioCodecItems)
      )
    )
    findViewById<Switch>(R.id.set_set_resolution).isChecked =
      appData.settings.getBoolean("setSetResolution", true)
    findViewById<Switch>(R.id.set_default_full).isChecked =
      appData.settings.getBoolean("setDefaultFull", true)
    findViewById<Switch>(R.id.set_float_nav).isChecked =
      appData.settings.getBoolean("setFloatNav", true)
  }

  // 设置返回按钮监听
  private fun setBackButtonListener() {
    findViewById<ImageView>(R.id.set_back).setOnClickListener {
      finish()
    }
  }

  // 设置视频编解码器监听
  private fun setVideoCodecListener() {
    val view = findViewById<Spinner>(R.id.set_videoCodec)
    view.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
          appData.settings.edit().apply {
            putString("setVideoCodec", view.selectedItem.toString())
            apply()
          }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

      }
  }

  // 设置音频编解码器监听
  private fun setAudioCodecListener() {
    val view = findViewById<Spinner>(R.id.set_audioCodec)
    view.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
          appData.settings.edit().apply {
            putString("setAudioCodec", view.selectedItem.toString())
            apply()
          }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

      }
  }

  // 设置是否修改分辨率监听
  private fun setSetResolutionListener() {
    findViewById<Switch>(R.id.set_set_resolution).setOnCheckedChangeListener { _, checked ->
      appData.settings.edit().apply {
        putBoolean("setSetResolution", checked)
        apply()
      }
    }
  }

  // 设置是否修改分辨率监听
  private fun setDefaultFullListener() {
    findViewById<Switch>(R.id.set_default_full).setOnCheckedChangeListener { _, checked ->
      appData.settings.edit().apply {
        putBoolean("setDefaultFull", checked)
        apply()
      }
    }
  }

  // 设置是否修改分辨率监听
  private fun setFloatNavListener() {
    findViewById<Switch>(R.id.set_float_nav).setOnCheckedChangeListener { _, checked ->
      appData.settings.edit().apply {
        putBoolean("setFloatNav", checked)
        apply()
      }
    }
  }

  // 设置导出按钮监听
  private fun setExportListener() {
    findViewById<TextView>(R.id.set_export).setOnClickListener {
      // 检查读写存储权限
      Toast.makeText(this, "UI按钮，别点", Toast.LENGTH_SHORT).show()
    }
  }

  // 设置导入按钮监听
  private fun setImportListener() {
    findViewById<TextView>(R.id.set_import).setOnClickListener {
      // 检查读写存储权限
      Toast.makeText(this, "UI按钮，别点", Toast.LENGTH_SHORT).show()
    }
  }
}