package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import top.saymzx.scrcpy.adb.AdbKeyPair
import top.saymzx.scrcpy.android.databinding.ActivitySetBinding
import top.saymzx.scrcpy.android.entity.Device
import java.io.File
import java.io.FileOutputStream

class SetActivity : Activity() {
  private lateinit var setActivity: ActivitySetBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setActivity = ActivitySetBinding.inflate(layoutInflater)
    setContentView(setActivity.root)
    appData.publicTools.setStatusAndNavBar(this)
    // 设置默认值
    setValue()
    // 设置返回按钮监听
    setBackButtonListener()
    // 设置默认设置监听
    setDefaultVideoCodecListener()
    setDefaultAudioCodecListener()
    setDefaultMaxSizeListener()
    setDefaultFpsListener()
    setDefaultVideoBitListener()
    setDefaultSetResolutionListener()
    // 显示
    setSlaveTurnOffScreenListener()
    setDefaultFullListener()
    setFloatNavSizeListener()
    setShowFps()
    // 其他
    setCheckUpdateListener()
    setClearDefultListener()
    setRenewKeyListener()
    // 备份恢复
    setKeyExportListener()
    setKeyImportListener()
    setJsonExportListener()
    setJsonImportListener()
    // 关于
    setIndexListener()
    setPrivacyListener()
    // 底栏
    setVersionName()
  }

  // 设置默认值
  @SuppressLint("SetTextI18n")
  private fun setValue() {
    setActivity.setDefaultVideoCodec.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoCodecItems))
    setActivity.setDefaultVideoCodec.setSelection(
      appData.publicTools.getStringIndex(
        appData.setValue.defaultVideoCodec, resources.getStringArray(R.array.videoCodecItems)
      )
    )
    setActivity.setDefaultAudioCodec.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.audioCodecItems))
    setActivity.setDefaultAudioCodec.setSelection(
      appData.publicTools.getStringIndex(
        appData.setValue.defaultAudioCodec, resources.getStringArray(R.array.audioCodecItems)
      )
    )
    setActivity.setDefaultMaxSize.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.maxSizeItems))
    setActivity.setDefaultMaxSize.setSelection(
      appData.publicTools.getStringIndex(
        appData.setValue.defaultMaxSize.toString(), resources.getStringArray(R.array.maxSizeItems)
      )
    )
    setActivity.setDefaultFps.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.fpsItems))
    setActivity.setDefaultFps.setSelection(
      appData.publicTools.getStringIndex(
        appData.setValue.defaultFps.toString(), resources.getStringArray(R.array.fpsItems)
      )
    )
    setActivity.setDefaultVideoBit.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoBitItems2))
    setActivity.setDefaultVideoBit.setSelection(
      appData.publicTools.getStringIndex(
        appData.setValue.defaultVideoBit.toString(),
        resources.getStringArray(R.array.videoBitItems1)
      )
    )
    setActivity.setDefaultSetResolution.isChecked = appData.setValue.defaultSetResolution
    setActivity.setSlaveTurnOffScreen.isChecked = appData.setValue.slaveTurnOffScreen
    setActivity.setDefaultDefaultFull.isChecked = appData.setValue.defaultFull
    val progress = appData.setValue.floatNavSize
    setActivity.setFloatNavSize.progress = progress - 35
    val preview = setActivity.setFloatNavPreview
    preview.layoutParams.apply {
      this.height = appData.publicTools.dp2px(progress.toFloat()).toInt()
      preview.layoutParams = this
    }
    setActivity.setShowFps.isChecked = appData.setValue.showFps
    setActivity.setCheckUpdate.isChecked = appData.setValue.checkUpdate
    setActivity.setVersionName.text = "当前版本: ${BuildConfig.VERSION_NAME}"
  }

  // 设置返回按钮监听
  private fun setBackButtonListener() {
    setActivity.setBack.setOnClickListener {
      finish()
    }
  }

  // 设置视频编解码器监听
  private fun setDefaultVideoCodecListener() {
    val view = setActivity.setDefaultVideoCodec
    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        appData.setValue.putDefaultVideoCodec(view.selectedItem.toString())
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }

  // 设置音频编解码器监听
  private fun setDefaultAudioCodecListener() {
    val view = setActivity.setDefaultAudioCodec
    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        appData.setValue.putDefaultAudioCodec(view.selectedItem.toString())
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }

  // 设置最大大小监听
  private fun setDefaultMaxSizeListener() {
    val view = setActivity.setDefaultMaxSize
    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        appData.setValue.putDefaultMaxSize(view.selectedItem.toString().toInt())
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }


  // 设置帧率监听
  private fun setDefaultFpsListener() {
    val view = setActivity.setDefaultFps
    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        appData.setValue.putDefaultFps(view.selectedItem.toString().toInt())
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }

  // 设置码率监听
  private fun setDefaultVideoBitListener() {
    val view = setActivity.setDefaultVideoBit
    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        appData.setValue.putDefaultVideoBit(resources.getStringArray(R.array.videoBitItems1)[view.selectedItemPosition].toInt())
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }

  // 设置是否修改分辨率监听
  private fun setDefaultSetResolutionListener() {
    setActivity.setDefaultSetResolution.setOnCheckedChangeListener { _, checked ->
      appData.setValue.putDefaultSetResolution(checked)
    }
  }

  // 设置被控端是否熄屏
  private fun setSlaveTurnOffScreenListener() {
    setActivity.setSlaveTurnOffScreen.setOnCheckedChangeListener { _, checked ->
      appData.setValue.putSlaveTurnOffScreen(checked)
    }
  }

  // 设置是否全屏监听
  private fun setDefaultFullListener() {
    setActivity.setDefaultDefaultFull.setOnCheckedChangeListener { _, checked ->
      appData.setValue.putDefaultFull(checked)
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
        appData.setValue.putFloatNavSize(progress)
      }

    })
  }

  // 是否显示刷新率
  private fun setShowFps() {
    setActivity.setShowFps.setOnCheckedChangeListener { _, checked ->
      appData.setValue.putShowFps(checked)
    }
  }

  // 设置是否检查更新
  private fun setCheckUpdateListener() {
    setActivity.setCheckUpdate.setOnCheckedChangeListener { _, checked ->
      appData.setValue.putCheckUpdate(checked)
    }
  }

  // 设置清除默认设备按钮监听
  private fun setClearDefultListener() {
    setActivity.setClearDefult.setOnClickListener {
      appData.setValue.putDefaultDevice("")
      Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
    }
  }

  // 设置重新生成密钥按钮监听
  private fun setRenewKeyListener() {
    setActivity.setRenewKey.setOnClickListener {
      AdbKeyPair.generate(appData.privateKey, appData.publicKey)
      appData.keyPair = AdbKeyPair.read(appData.privateKey, appData.publicKey)
      Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
    }
  }

  // 设置密钥导出按钮监听
  private fun setKeyExportListener() {
    setActivity.setExportKey.setOnClickListener {
      openDirectory(1)
    }
  }

  // 设置密钥导入按钮监听
  private fun setKeyImportListener() {
    setActivity.setImportKey.setOnClickListener {
      openDirectory(2)
    }
  }

  // 设置密钥导出按钮监听
  private fun setJsonExportListener() {
    setActivity.setExportJson.setOnClickListener {
      openDirectory(3)
    }
  }

  // 设置密钥导入按钮监听
  private fun setJsonImportListener() {
    setActivity.setImportJson.setOnClickListener {
      openDirectory(4)
    }
  }

  // 设置隐私政策按钮监听
  private fun setPrivacyListener() {
    setActivity.setPrivacy.setOnClickListener {
      try {
        // 防止没有默认浏览器
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://github.com/mingzhixian/scrcpy/blob/master/PRIVACY.md")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
  }

  // 设置官网按钮监听
  private fun setIndexListener() {
    setActivity.setIndex.setOnClickListener {
      try {
        // 防止没有默认浏览器
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://scrcpy.saymzx.top/")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
  }

  // 底栏
  private fun setVersionName() {
    setActivity.setVersionName.setOnClickListener {
      appData.publicTools.checkUpdate(this,true)
    }
  }

  // 检查存储权限
  private fun openDirectory(mode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    // 操作类型(1为密钥导出，2为密钥导入，3为Json导出，4为密钥导入)
    startActivityForResult(intent, mode)
    Toast.makeText(this, "请不要选择Download或其他隐私位置", Toast.LENGTH_LONG).show()
  }

  override fun onActivityResult(
    requestCode: Int, resultCode: Int, resultData: Intent?
  ) {
    if (resultCode == RESULT_OK) {
      resultData?.data?.also { uri ->
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        if (documentFile == null) {
          Toast.makeText(this, "空地址", Toast.LENGTH_SHORT).show()
          return
        }
        try {
          when (requestCode) {
            // 密钥导出
            1 -> {
              val privateKeyDoc = documentFile.findFile("scrcpy_private.key")
              val privateKeyUri = privateKeyDoc?.uri ?: documentFile.createFile(
                "scrcpy/key", "scrcpy_private.key"
              )!!.uri
              writeToFile(appData.privateKey, privateKeyUri, 2)
              val publicKeyDoc = documentFile.findFile("scrcpy_public.key")
              val publicKeyUri =
                publicKeyDoc?.uri ?: documentFile.createFile(
                  "scrcpy/key",
                  "scrcpy_public.key"
                )!!.uri
              writeToFile(appData.publicKey, publicKeyUri, 2)
            }
            // 密钥导入
            2 -> {
              val privateKeyDoc = documentFile.findFile("scrcpy_private.key")
              val publicKeyDoc = documentFile.findFile("scrcpy_public.key")
              // 检查文件是否存在
              if (privateKeyDoc == null || publicKeyDoc == null) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
              }
              readFile(appData.privateKey, privateKeyDoc.uri, 2)
              readFile(appData.publicKey, publicKeyDoc.uri, 2)
            }
            // Json导出
            3 -> {
              val dataBaseDoc = documentFile.findFile("scrcpy_database.json")
              val dataBaseUri = dataBaseDoc?.uri ?: documentFile.createFile(
                "scrcpt/json", "scrcpy_database.json"
              )!!.uri
              val devicesJson = JSONArray()
              for (i in appData.devices) devicesJson.put(i.toJson())
              val setVlaueJson = appData.setValue.toJson()
              val json = JSONObject()
              json.put("setVlaue", setVlaueJson)
              json.put("devices", devicesJson)
              writeToFile(json.toString(), dataBaseUri, 1)
            }
            // Json导入
            4 -> {
              val dataBaseDoc = documentFile.findFile("scrcpy_database.json")
              // 检查文件是否存在
              if (dataBaseDoc == null) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
              }
              val json = JSONObject(readFile(null, dataBaseDoc.uri, 1))
              val devicesJson = json.getJSONArray("devices")
              val setVlaueJson = json.getJSONObject("setVlaue")
              for (i in 0 until devicesJson.length()) {
                appData.deviceListAdapter.newDevice(
                  Device(devicesJson.getJSONObject(i))
                )
              }
              appData.setValue.fromJson(setVlaueJson)
            }
          }
        } catch (_: Exception) {
        }
      }
    }
  }

  // 写入文件(mode为1写入string，mode为2传入File)
  private fun writeToFile(data: Any, uri: Uri, mode: Int) {
    try {
      contentResolver.openFileDescriptor(uri, "w")?.use {
        FileOutputStream(it.fileDescriptor).use {
          if (mode == 1) {
            it.write((data as String).toByteArray())
          } else if (mode == 2) {
            val byteArray = ByteArray(512)
            val fileInputStream = (data as File).inputStream()
            while (true) {
              val len = fileInputStream.read(byteArray)
              if (len < 0) break
              it.write(byteArray, 0, len)
            }
          }
        }
      }
    } catch (_: Exception) {
    }
  }

  // 读取文件(mode为1返回string，mode为2直接写入File)
  private fun readFile(file: File?, uri: Uri, mode: Int): String {
    try {
      contentResolver.openInputStream(uri)?.use {
        if (mode == 1) {
          var str = ""
          val byteArray = ByteArray(512)
          while (true) {
            val len = it.read(byteArray)
            if (len < 0) {
              return str
            }
            str += String(byteArray, 0, len)
          }
        } else if (mode == 2) {
          val byteArray = ByteArray(512)
          val fileOutputStream = file!!.outputStream()
          while (true) {
            val len = it.read(byteArray)
            if (len < 0) return ""
            fileOutputStream.write(byteArray, 0, len)
          }
        }
      }
    } catch (_: Exception) {
    }
    return ""
  }
}