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
import top.saymzx.scrcpy.adb.AdbKeyPair
import top.saymzx.scrcpy.android.databinding.ActivitySetBinding
import top.saymzx.scrcpy.android.entity.Device
import top.saymzx.scrcpy.android.entity.defaultAudioCodec
import top.saymzx.scrcpy.android.entity.defaultFps
import top.saymzx.scrcpy.android.entity.defaultFull
import top.saymzx.scrcpy.android.entity.defaultMaxSize
import top.saymzx.scrcpy.android.entity.defaultSetResolution
import top.saymzx.scrcpy.android.entity.defaultVideoBit
import top.saymzx.scrcpy.android.entity.defaultVideoCodec
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

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
    setDefaultFullListener()
    setFloatNavSizeListener()
    // 其他
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
  }

  // 设置默认值
  private fun setValue() {
    setActivity.setDefaultVideoCodec.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoCodecItems))
    setActivity.setDefaultVideoCodec.setSelection(
      appData.publicTools.getStringIndex(
        defaultVideoCodec, resources.getStringArray(R.array.videoCodecItems)
      )
    )
    setActivity.setDefaultAudioCodec.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.audioCodecItems))
    setActivity.setDefaultAudioCodec.setSelection(
      appData.publicTools.getStringIndex(
        defaultAudioCodec, resources.getStringArray(R.array.audioCodecItems)
      )
    )
    setActivity.setDefaultMaxSize.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.maxSizeItems))
    setActivity.setDefaultMaxSize.setSelection(
      appData.publicTools.getStringIndex(
        defaultMaxSize.toString(), resources.getStringArray(R.array.maxSizeItems)
      )
    )
    setActivity.setDefaultFps.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.fpsItems))
    setActivity.setDefaultFps.setSelection(
      appData.publicTools.getStringIndex(
        defaultFps.toString(), resources.getStringArray(R.array.fpsItems)
      )
    )
    setActivity.setDefaultVideoBit.adapter =
      ArrayAdapter(this, R.layout.spinner_item, resources.getStringArray(R.array.videoBitItems2))
    setActivity.setDefaultVideoBit.setSelection(
      appData.publicTools.getStringIndex(
        defaultVideoBit.toString(), resources.getStringArray(R.array.videoBitItems1)
      )
    )
    setActivity.setDefaultSetResolution.isChecked = defaultSetResolution
    setActivity.setDefaultDefaultFull.isChecked = defaultFull
    val progress = appData.settings.getInt("floatNavSize", 55)
    setActivity.setFloatNavSize.progress = progress - 35
    val preview = setActivity.setFloatNavPreview
    preview.layoutParams.apply {
      this.height = appData.publicTools.dp2px(progress.toFloat()).toInt()
      preview.layoutParams = this
    }
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
        val item = view.selectedItem.toString()
        defaultVideoCodec = item
        appData.settings.edit().apply {
          putString("defaultVideoCodec", item)
          apply()
        }
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
        val item = view.selectedItem.toString()
        defaultAudioCodec = item
        appData.settings.edit().apply {
          putString("defaultAudioCodec", item)
          apply()
        }
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
        val item = view.selectedItem.toString()
        defaultMaxSize = item.toInt()
        appData.settings.edit().apply {
          putInt("defaultMaxSize", item.toInt())
          apply()
        }
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
        val item = view.selectedItem.toString()
        defaultFps = item.toInt()
        appData.settings.edit().apply {
          putInt("defaultFps", item.toInt())
          apply()
        }
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
        defaultVideoBit =
          resources.getStringArray(R.array.videoBitItems1)[view.selectedItemPosition].toInt()
        appData.settings.edit().apply {
          putInt("defaultVideoBit", defaultVideoBit)
          apply()
        }
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

    }
  }

  // 设置是否修改分辨率监听
  private fun setDefaultSetResolutionListener() {
    setActivity.setDefaultSetResolution.setOnCheckedChangeListener { _, checked ->
      defaultSetResolution = checked
      appData.settings.edit().apply {
        putBoolean("defaultSetResolution", checked)
        apply()
      }
    }
  }

  // 设置是否全屏监听
  private fun setDefaultFullListener() {
    setActivity.setDefaultDefaultFull.setOnCheckedChangeListener { _, checked ->
      defaultFull = checked
      appData.settings.edit().apply {
        putBoolean("defaultFull", checked)
        apply()
      }
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
        appData.settings.edit().apply {
          putInt("floatNavSize", progress)
          apply()
        }
      }

    })
  }

  // 设置清除默认设备按钮监听
  private fun setClearDefultListener() {
    setActivity.setClearDefult.setOnClickListener {
      appData.settings.edit().apply {
        putString("DefaultDevice", "")
        apply()
      }
      Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
    }
  }

  // 设置重新生成密钥按钮监听
  private fun setRenewKeyListener() {
    setActivity.setRenewKey.setOnClickListener {
      AdbKeyPair.generate(appData.privateKey, appData.publicKey)
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

  // 检查存储权限
  private fun openDirectory(mode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    // 操作类型(1为密钥导出，2为密钥导入，3为Json导出，4为密钥导入)
    startActivityForResult(intent, mode)
    Toast.makeText(this, "请不要选择Download或其他隐私位置", Toast.LENGTH_LONG).show()
  }

  @SuppressLint("Range")
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
              publicKeyDoc?.uri ?: documentFile.createFile("scrcpy/key", "scrcpy_public.key")!!.uri
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
            val jsonArray = JSONArray()
            for (i in appData.devices) jsonArray.put(i.toJson())
            writeToFile(jsonArray.toString(), dataBaseUri, 1)
          }
          // Json导入
          4 -> {
            val dataBaseDoc = documentFile.findFile("scrcpy_database.json")
            // 检查文件是否存在
            if (dataBaseDoc == null) {
              Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
              return
            }
            val jsonArray = JSONArray(readFile(null, dataBaseDoc.uri, 1))
            for (i in 0 until jsonArray.length()) {
              appData.deviceListAdapter.newDevice(
                Device(jsonArray.getJSONObject(i))
              )
            }
          }
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
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    } catch (e: IOException) {
      e.printStackTrace()
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
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return ""
  }
}