package top.saymzx.easycontrol.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import top.saymzx.easycontrol.app.databinding.ActivityMainBinding
import top.saymzx.easycontrol.app.helper.AppData

lateinit var appData: AppData

class MainActivity : AppCompatActivity() {

  private lateinit var mainActivity: ActivityMainBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mainActivity = ActivityMainBinding.inflate(layoutInflater)
    setContentView(mainActivity.root)
    appData = AppData(this)
    // 设置状态栏导航栏颜色沉浸
    appData.publicTools.setStatusAndNavBar(this)
    // 如果第一次使用进入软件展示页
    if (appData.settings.getBoolean("FirstUse", true)) startActivityForResult(
      Intent(
        this, ShowAppActivity::class.java
      ), 1
    )
    else readMode()
  }

  // 其他页面回调
  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // ShowApp页面回调
    if (requestCode == 1) {
      readMode()
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  // 软件模式
  private fun readMode() {
    if (appData.setValue.appMode == 1) {
      startActivity(Intent(this, MasterActivity::class.java))
    } else {
      startActivity(Intent(this, SlaveActivity::class.java))
    }
  }

}