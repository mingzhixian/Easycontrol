package top.saymzx.easycontrol.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import top.saymzx.easycontrol.adb.AdbKeyPair
import top.saymzx.easycontrol.app.databinding.ActivitySetBinding
import top.saymzx.easycontrol.app.entity.Device
import top.saymzx.easycontrol.app.entity.Setting
import java.io.File
import java.io.FileOutputStream

class SetActivity : AppCompatActivity() {
  private lateinit var setActivity: ActivitySetBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setActivity = ActivitySetBinding.inflate(layoutInflater)
    setContentView(setActivity.root)
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
    val progress = appData.setting.floatNavSize
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
    setActivity.setOther.addView(
      appData.publicTools.createTextCard(
        this,
        "备份"
      ) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1)
      }.root
    )
    setActivity.setOther.addView(
      appData.publicTools.createTextCard(
        this,
        "恢复"
      ) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 2)
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
          intent.data = Uri.parse("https://scrcpy.saymzx.top/")
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
        appData.netHelper.getJson("https://github.saymzx.top/api/repos/mingzhixian/scrcpy/releases/latest") {
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
        appData.setting.floatNavSize = progress
      }

    })
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Deprecated("Deprecated in Java")
  override fun onActivityResult(
    requestCode: Int, resultCode: Int, resultData: Intent?
  ) {
    if (resultCode == RESULT_OK) {
      resultData?.data?.also { uri ->
        val documentFile = DocumentFile.fromTreeUri(this, uri) ?: return
        try {
          if (requestCode == 1) {
            writeToFile("easycontrol_private.key", appData.privateKey, documentFile, 2)
            writeToFile("easycontrol_public.key", appData.publicKey, documentFile, 2)
            writeToFile("easycontrol_database.json", appData.dbHelper.devices().)
            writeToFile(
              "easycontrol_setting.json",
              Json.encodeToString(appData.setting),
              documentFile,
              1
            )
          } else {
            readFormFile("easycontrol_private.key", appData.privateKey, documentFile, 2)
            readFormFile("easycontrol_public.key", appData.privateKey, documentFile, 2)
            appData.setting = Json.decodeFromString(
              readFormFile(
                "easycontrol_setting.json",
                null,
                documentFile,
                1
              )
            )
          }
          when (requestCode) {
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
    super.onActivityResult(requestCode, resultCode, resultData)
  }

  // 写入文件(mode为1写入string，mode为2传入File)
  private fun writeToFile(fileName: String, data: Any, documentFile: DocumentFile, mode: Int) {
    try {
      val uri = documentFile.findFile(fileName)?.uri ?: documentFile.createFile(
        "easycontrol/file", fileName
      )!!.uri
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
  private fun readFormFile(
    fileName: String,
    file: File?,
    documentFile: DocumentFile,
    mode: Int
  ): String {
    try {
      val doc = documentFile.findFile(fileName)
      if (doc == null) {
        Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
        return ""
      }
      contentResolver.openInputStream(doc.uri)?.use {
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