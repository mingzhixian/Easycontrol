package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class SetActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_set)
    appData.publicTools.setStatusAndNavBar(this)
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
    // 设置清除默认设备按钮监听
    setClearDefultListener()
    // 设置导出按钮监听
    setExportListener()
    // 设置导入按钮监听
    setImportListener()
    // 设置官网按钮监听
    setIndexListener()
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

  // 设置是否全屏监听
  private fun setDefaultFullListener() {
    findViewById<Switch>(R.id.set_default_full).setOnCheckedChangeListener { _, checked ->
      appData.settings.edit().apply {
        putBoolean("setDefaultFull", checked)
        apply()
      }
    }
  }

  // 设置清除默认设备按钮监听
  private fun setClearDefultListener() {
    findViewById<TextView>(R.id.set_clear_defult).setOnClickListener {
      appData.settings.edit().apply {
        putString("DefaultDevice", "")
        apply()
      }
      Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
    }
  }

  // 设置导出按钮监听
  private fun setExportListener() {
    findViewById<TextView>(R.id.set_export).setOnClickListener {
      openDirectory()
      fileMode = 1
    }
  }

  // 设置导入按钮监听
  private fun setImportListener() {
    findViewById<TextView>(R.id.set_import).setOnClickListener {
      openDirectory()
      fileMode = 2
    }
  }

  // 设置官网按钮监听
  private fun setIndexListener() {
    findViewById<TextView>(R.id.set_index).setOnClickListener {
      try {
        // 防止没有默认浏览器
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse("https://github.com/mingzhixian/NovelNeo")
        startActivity(intent)
      } catch (_: Exception) {
      }
    }
  }

  // 检查存储权限
  private fun openDirectory() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    startActivityForResult(intent, 22)
    Toast.makeText(this, "请不要选择Download或其他隐私位置", Toast.LENGTH_LONG).show()
  }

  // 操作类型(1为导出，2为导入)
  private var fileMode = 1

  @SuppressLint("Range")
  override fun onActivityResult(
    requestCode: Int, resultCode: Int, resultData: Intent?
  ) {
    if (requestCode == 22 && resultCode == RESULT_OK) {
      resultData?.data?.also { uri ->
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        // 显示加载中
        val alert = appData.publicTools.showLoading("请等待...", this, false, null)
        // 导出
        if (documentFile == null) {
          alert.cancel()
          Toast.makeText(this, "空地址", Toast.LENGTH_SHORT).show()
          return
        }
        if (fileMode == 1) {
          val privateKeyDoc = documentFile.findFile("scrcpy_private.key")
          val privateKeyUri =
            privateKeyDoc?.uri ?: documentFile.createFile("scrcpt/key", "scrcpy_private.key")!!.uri
          writeToFile(appData.privateKey, privateKeyUri, 2)
          val publicKeyDoc = documentFile.findFile("scrcpy_public.key")
          val publicKeyUri =
            publicKeyDoc?.uri ?: documentFile.createFile("scrcpt/key", "scrcpy_public.key")!!.uri
          writeToFile(appData.privateKey, publicKeyUri, 2)
          val dataBaseDoc = documentFile.findFile("scrcpy_database.json")
          val dataBaseUri =
            dataBaseDoc?.uri ?: documentFile.createFile("scrcpt/json", "scrcpy_database.json")!!.uri
          dataBaseUri.let {
            val jsonArray = JSONArray()
            // 从数据库获取设备列表
            val cursor = appData.dbHelper.readableDatabase.query(
              "DevicesDb",
              null,
              null,
              null,
              null,
              null,
              null
            )
            if (cursor.moveToFirst()) {
              do {
                val tmpJsonObject = JSONObject()
                tmpJsonObject.put("name", cursor.getString(cursor.getColumnIndex("name")))
                tmpJsonObject.put("address", cursor.getString(cursor.getColumnIndex("address")))
                tmpJsonObject.put("port", cursor.getInt(cursor.getColumnIndex("port")))
                tmpJsonObject.put(
                  "videoCodec",
                  cursor.getString(cursor.getColumnIndex("videoCodec"))
                )
                tmpJsonObject.put(
                  "audioCodec",
                  cursor.getString(cursor.getColumnIndex("audioCodec"))
                )
                tmpJsonObject.put("maxSize", cursor.getInt(cursor.getColumnIndex("maxSize")))
                tmpJsonObject.put("fps", cursor.getInt(cursor.getColumnIndex("fps")))
                tmpJsonObject.put("videoBit", cursor.getInt(cursor.getColumnIndex("videoBit")))
                tmpJsonObject.put(
                  "setResolution",
                  cursor.getInt(cursor.getColumnIndex("setResolution"))
                )
                tmpJsonObject.put(
                  "defaultFull",
                  cursor.getInt(cursor.getColumnIndex("defaultFull"))
                )
                jsonArray.put(tmpJsonObject)
              } while (cursor.moveToNext())
            }
            cursor.close()
            writeToFile(jsonArray.toString(), it, 1)
          }
          alert.cancel()
        }
        // 导入
        else if (fileMode == 2) {
          // 检查文件是否存在
          val privateKeyDoc = documentFile.findFile("scrcpy_private.key")
          val publicKeyDoc = documentFile.findFile("scrcpy_public.key")
          val dataBaseDoc = documentFile.findFile("scrcpy_database.json")
          if (privateKeyDoc == null || publicKeyDoc == null || dataBaseDoc == null) {
            alert.cancel()
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
            return
          }
          readFile(appData.privateKey, privateKeyDoc.uri, 2)
          readFile(appData.publicKey, publicKeyDoc.uri, 2)
          val jsonArray = JSONArray(readFile(null, dataBaseDoc.uri, 1))
          for (i in 0 until jsonArray.length()) {
            val tmpJsonObject = jsonArray.getJSONObject(i)
            appData.deviceListAdapter.newDevice(
              tmpJsonObject.getString("name"),
              tmpJsonObject.getString("address"),
              tmpJsonObject.getInt("port"),
              tmpJsonObject.getString("videoCodec"),
              tmpJsonObject.getString("audioCodec"),
              tmpJsonObject.getInt("maxSize"),
              tmpJsonObject.getInt("fps"),
              tmpJsonObject.getInt("videoBit"),
              tmpJsonObject.getInt("setResolution") == 1,
              tmpJsonObject.getInt("defaultFull") == 1
            )
          }
          alert.cancel()
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