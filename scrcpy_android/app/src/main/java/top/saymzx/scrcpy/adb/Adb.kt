package top.saymzx.scrcpy.adb

import okio.sink
import okio.source
import java.io.IOException
import java.net.Socket
import java.util.Random

class Adb(host: String, port: Int, keyPair: AdbKeyPair) {

  private val socket: Socket
  private val adbWriter: AdbWriter
  private val adbReader: AdbReader
  private val random = Random()
  private val maxPayloadSize: Int
  private val connectionStreams = HashMap<Int, AdbStream>()
  private val connectionStatus = HashMap<Int, Int>() // 0为断开，1为等待回复中，2为读写中

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
    maxPayloadSize = message.arg1
    // 开始读写线程
    Thread { readAdb() }.start()
  }

  // 打开一个连接流
  fun open(destination: String): AdbStream {
    val localId = random.nextInt()
    adbWriter.writeOpen(localId, destination)
    connectionStatus[localId] = 1
    val stream = AdbStream(maxPayloadSize, adbWriter)
    stream.localId = localId
    connectionStreams[localId] = stream
    // 等待2秒
    for (i in 0..200) {
      when (connectionStatus[localId]) {
        2 -> {
          return stream
        }

        else -> {
          Thread.sleep(10)
        }
      }
    }
    throw IOException("连接错误")
  }

  fun runAdbCmd(cmd: String): String {
    val stream = open("shell:$cmd")
    // 等待4秒
    for (i in 0..200) {
      if (connectionStatus[stream.localId] == 0) {
        return stream.source.readUtf8()
      } else {
        Thread.sleep(20)
      }
    }
    return ""
  }

  fun tcpForward(port: Int): AdbStream {
    val stream = open("tcp:$port")
    // 等待4秒
    for (i in 0..200) {
      if (connectionStatus[stream.localId] == 2) {
        return stream
      } else {
        Thread.sleep(20)
      }
    }
    throw IOException("连接错误")
  }

  // 读取线程
  private fun readAdb() {
    try {
      while (true) {
        val message = adbReader.readMessage()
        when (message.command) {
          Constants.CMD_OKAY -> {
            if (connectionStreams[message.arg1]!!.remoteId == 0)
              connectionStreams[message.arg1]!!.remoteId = message.arg0
            connectionStreams[message.arg1]?.push(message.payload)
            connectionStatus[message.arg1] = 2
          }

          Constants.CMD_WRTE -> {
            adbWriter.writeOkay(message.arg1, message.arg0)
            connectionStreams[message.arg1]?.push(message.payload)
          }

          Constants.CMD_CLSE -> {
          }
        }
      }
    } catch (_: Exception) {
      close()
    }
  }

  fun close() {
    adbReader.close()
    adbWriter.close()
    socket.close()
  }
}