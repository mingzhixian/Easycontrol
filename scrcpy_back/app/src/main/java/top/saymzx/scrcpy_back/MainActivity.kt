package top.saymzx.scrcpy_back

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.File
import java.net.Socket
import javax.xml.bind.DatatypeConverter

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, Page2::class.java)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags =
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        Thread { back() }.start()
        startActivity(intent)
    }

    private fun back() {
        val remoteIp = "127.0.0.1"
        val remotePort = 5555
        // 读取或创建保存配对密钥
        val public = File(applicationContext.filesDir, "public.key")
        val private = File(applicationContext.filesDir, "private.key")
        val crypto: AdbCrypto
        if (public.isFile && private.isFile) {
            crypto = AdbCrypto.loadAdbKeyPair({ data: ByteArray? ->
                DatatypeConverter.printBase64Binary(data)
            }, private, public)
        } else {
            crypto =
                AdbCrypto.generateAdbKeyPair { data -> DatatypeConverter.printBase64Binary(data) }
            crypto.saveAdbKeyPair(private, public)
        }
        // 连接ADB
        val socket = Socket(remoteIp, remotePort)
        val connection = AdbConnection.create(socket, crypto)
        connection.connect()
        val stream = connection.open("shell:")
        stream.write(" ps aux | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9" + '\n')
        stream.write(" wm size reset " + '\n')
        Thread.sleep(100)
        stream.close()
        finishAndRemoveTask()
        Runtime.getRuntime().exit(0)
    }
}

class QuickStartTileService : TileService()