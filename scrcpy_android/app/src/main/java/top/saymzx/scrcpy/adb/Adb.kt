/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.scrcpy.adb

import android.util.Log
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import okio.Buffer
import okio.sink
import okio.source
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Random

class Adb(host: String, port: Int, keyPair: AdbKeyPair) {

  private val socket: Socket
  private val adbWriter: AdbWriter
  private val adbReader: AdbReader
  private val random = Random()
  private val connectionStreams = HashMap<Int, AdbStream>()
  private var defaultShellStream: AdbStream? = null

  init {
    // 连接socket
    socket = Socket(host, port)
    // 读写工具
    adbReader = AdbReader(socket.source())
    adbWriter = AdbWriter(socket.sink())
    // 连接ADB
    adbWriter.writeConnect()
    var message = adbReader.readMessage()
    // 身份认证
    if (message.command == Constants.CMD_AUTH) {
      adbWriter.writeAuth(Constants.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message))
      message = adbReader.readMessage()
      if (message.command == Constants.CMD_AUTH) {
        adbWriter.writeAuth(Constants.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes)
        message = adbReader.readMessage()
      }
    }
    // 认证失败
    if (message.command != Constants.CMD_CNXN) throw IOException("Connection failed: $message")
    // 最大承载长度
    adbWriter.maxPayloadSize = message.arg1
    // 开始读写线程
    val thread = Thread { readAdbThread() }
    thread.priority = Thread.MAX_PRIORITY
    thread.start()
  }

  // 打开一个连接流
  fun open(destination: String, isNeedSource: Boolean): AdbStream {
    val localId = random.nextInt()
    adbWriter.writeOpen(localId, destination)
    val stream = AdbStream(localId, adbWriter, isNeedSource)
    connectionStreams[localId] = stream
    synchronized(stream) {
      stream.wait()
    }
    if (stream.status == -1) throw IOException("连接错误")
    else return stream
  }

  fun pushFile(file: InputStream, remotePath: String) {
    val pushStream = open("sync:start a SYNC service", true)
    val sendString = "$remotePath,33206"
    pushStream.write(createSendPacket("SEND", sendString.length) + sendString.toByteArray())
    val byteArray = ByteArray(60000)
    var len = file.read(byteArray, 0, byteArray.size)
    do {
      var tmpByteArray = createSendPacket("DATA", len) + byteArray.sliceArray(0 until len)
      if (len < byteArray.size) {
        tmpByteArray += createSendPacket("DONE", System.currentTimeMillis().toInt())
      }
      pushStream.write(tmpByteArray)
      len = file.read(byteArray, 0, byteArray.size)
    } while (len > 0)
    pushStream.write(createSendPacket("QUIT", 0))
    synchronized(pushStream) {
      pushStream.wait()
    }
  }

  private fun createSendPacket(id: String, arg: Int): ByteArray {
    val tmpBuffer = Buffer()
    tmpBuffer.clear()
    tmpBuffer.writeString(id, StandardCharsets.UTF_8)
    tmpBuffer.writeIntLe(arg)
    return tmpBuffer.readByteArray()
  }

  fun runAdbCmd(cmd: String, isNeedOutput: Boolean): String {
    if (!isNeedOutput) {
      if (defaultShellStream == null) defaultShellStream = open("shell:", false)
      defaultShellStream!!.write((cmd + "\n").toByteArray())
      return ""
    } else {
      val stream = open("shell:$cmd", true)
      // 等待8秒
      for (i in 0..200) {
        if (stream.status == -1) {
          return stream.source.readUtf8()
        } else {
          Thread.sleep(40)
        }
      }
      return "命令运行超过8秒，未获取命令输出"
    }
  }

  fun tcpForward(port: Int, isNeedSource: Boolean): AdbStream = open("tcp:$port", isNeedSource)

  fun localSocketForward(socketName: String, isNeedSource: Boolean): AdbStream =
    open("localabstract:$socketName", isNeedSource)

  // 读取线程
  private fun readAdbThread() {
    try {
      while (true) {
        val message = adbReader.readMessage()
        val stream = connectionStreams[message.arg1]!!
        when (message.command) {
          Constants.CMD_OKAY -> {
            stream.status = 2
            // 连接成功
            if (stream.remoteId == 0) {
              stream.remoteId = message.arg0
              synchronized(stream) {
                stream.notifyAll()
              }
            }
            synchronized(stream.canWrite) { stream.canWrite.notify() }
          }

          Constants.CMD_WRTE -> {
            adbWriter.writeOkay(message.arg1, message.arg0)
            stream.pushToSource(message.payload)
          }

          Constants.CMD_CLSE -> {
            // 连接断开
            stream.status = -1
            synchronized(stream) {
              stream.notifyAll()
            }
          }
        }
      }
    } catch (_: Exception) {
      close()
    }
  }

  fun close() {
    try {
      defaultShellStream?.close()
      adbReader.close()
      adbWriter.close()
      socket.close()
    } catch (_: Exception) {
    }
  }
}