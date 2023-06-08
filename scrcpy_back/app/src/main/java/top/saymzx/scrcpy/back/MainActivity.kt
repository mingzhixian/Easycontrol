package top.saymzx.scrcpy.back

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

class MainActivity : Activity() {
    lateinit var settings: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 全屏显示
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        // 设置异形屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        // 隐藏标题栏
        actionBar?.hide()
        settings = getSharedPreferences("setting", Context.MODE_PRIVATE)
        // 首次打开
        if (settings.getBoolean("FirstUse", true)) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            val editPortDialog = builder.create()
            editPortDialog.setCanceledOnTouchOutside(false)
            editPortDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val editPort = LayoutInflater.from(this).inflate(R.layout.edit_port, null, false)
            editPortDialog.setView(editPort)
            // 设置监听
            editPort.findViewById<Button>(R.id.edit_port_ok).setOnClickListener {
                settings.edit().apply {
                    putBoolean("FirstUse", false)
                    putInt(
                        "Port",
                        editPort.findViewById<EditText>(R.id.edit_port_port).text.toString().toInt()
                    )
                    apply()
                }
                editPortDialog.cancel()
                startBack()
            }
            editPortDialog.show()
        } else startBack()

    }

    private fun startBack() {
        val intent = Intent(this, Page2::class.java)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags =
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        Thread {
            val ip = "127.0.0.1"
            val port = settings.getInt("Port", 5555)
            // 读取或创建保存配对密钥
            val publicKey = File(applicationContext.filesDir, "public.key")
            val privateKey = File(applicationContext.filesDir, "private.key")
            if (!privateKey.isFile || !publicKey.isFile) {
                AdbKeyPair.generate(privateKey, publicKey)
            }
            val adb = Dadb.create(ip, port, AdbKeyPair.read(privateKey, publicKey))
            Log.i(
                "scrcpy_back",
                adb.shell("ps aux | grep scrcpy | grep -v grep | awk '{print \$2}' | xargs kill -9").allOutput
            )
            Log.i("scrcpy_back", adb.shell("wm size reset").allOutput)
            adb.close()
            runOnUiThread { Toast.makeText(this, "恢复程序执行完毕", Toast.LENGTH_LONG).show() }
            finishAndRemoveTask()
            Runtime.getRuntime().exit(0)
        }.start()
        startActivity(intent)
    }
}

class QuickStartTileService : TileService()