package top.saymzx.easycontrol.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.easycontrol.adb.Adb
import top.saymzx.easycontrol.app.databinding.ActivitySlaveBinding
import java.net.NetworkInterface

class SlaveActivity : Activity() {
  private lateinit var slaveActivity: ActivitySlaveBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    slaveActivity = ActivitySlaveBinding.inflate(layoutInflater)
    setContentView(slaveActivity.root)
    appData.main = this
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 绘制UI
    drawUi()
    // 设置ADB端口修改监听
    setAdbPortListener()
    // 设置恢复按钮监听
    setBackButtonListener()
    // 自动恢复
    autoBack()
  }

  private fun drawUi() {
    // 添加IP
    val listPair = getIp()
    for (i in listPair.first) {
      val text = appData.publicTools.createTextCard(this, i) {
        appData.clipBoard.setPrimaryClip(
          ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i)
        )
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
      }
      slaveActivity.slaveIpv4.addView(text.root)
    }
    for (i in listPair.second) {
      val text = appData.publicTools.createTextCard(this, i) {
        appData.clipBoard.setPrimaryClip(
          ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i)
        )
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
      }
      slaveActivity.slaveIpv6.addView(text.root)
    }
    // 添加自动恢复开关设置
    slaveActivity.slaveAutoBack.addView(
      appData.publicTools.createSwitchCard(this, "自动恢复", appData.setting.slaveAutoBack) {
        appData.setting.slaveAutoBack = it
      }.root
    )
    // 设置ADB端口
    slaveActivity.slavePort.setText(appData.setting.slaveAdbPort.toString())
  }

  private fun setAdbPortListener() {
    slaveActivity.slavePort.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      }

      override fun afterTextChanged(editable: Editable?) {
        val text = editable.toString()
        if (text == "") return
        appData.setting.slaveAdbPort = text.toInt()
      }
    })
  }

  private fun setBackButtonListener() {
    slaveActivity.slaveBackButton.setOnClickListener {
      back(
        "127.0.0.1",
        appData.setting.slaveAdbPort
      )
    }
  }

  private fun autoBack() {
    if (isNeedAutoBack && appData.setting.slaveAutoBack) {
      back("127.0.0.1", appData.setting.slaveAdbPort)
    }
  }

  // 获取IP地址
  private fun getIp(): Pair<List<String>, List<String>> {
    val ipv4Addresses = mutableListOf<String>()
    val ipv6Addresses = mutableListOf<String>()

    try {
      val networkInterfaces = NetworkInterface.getNetworkInterfaces()
      while (networkInterfaces.hasMoreElements()) {
        val networkInterface = networkInterfaces.nextElement()
        val inetAddresses = networkInterface.inetAddresses
        while (inetAddresses.hasMoreElements()) {
          val inetAddress = inetAddresses.nextElement()
          if (!inetAddress.isLoopbackAddress)
            if (inetAddress is java.net.Inet4Address) ipv4Addresses.add(inetAddress.hostAddress!!)
            else if (inetAddress is java.net.Inet6Address && !inetAddress.isLinkLocalAddress)
              ipv6Addresses.add(inetAddress.hostAddress!!)
        }
      }
    } catch (_: Exception) {
    }

    return Pair(ipv4Addresses, ipv6Addresses)
  }

  companion object {
    var isNeedAutoBack = true
    fun back(ip: String, port: Int) {
      appData.mainScope.launch {
        withContext(Dispatchers.IO) {
          try {
            isNeedAutoBack = false
            val adb = Adb(
              ip,
              port,
              appData.keyPair
            )
            adb.runAdbCmd(
              "ps -ef | grep top.saymzx.easycontrol.server.Server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9",
              false
            )
            adb.runAdbCmd("wm size reset", false)
            adb.close()
          } catch (_: Exception) {
            withContext(Dispatchers.Main) {
              Toast.makeText(appData.main, "连接失败", Toast.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  }
}