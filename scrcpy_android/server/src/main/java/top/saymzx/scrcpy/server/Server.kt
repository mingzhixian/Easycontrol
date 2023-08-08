package top.saymzx.scrcpy.server

import android.net.LocalServerSocket
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import top.saymzx.scrcpy.server.entity.Device
import top.saymzx.scrcpy.server.entity.Options
import top.saymzx.scrcpy.server.helper.AudioEncode
import top.saymzx.scrcpy.server.helper.Controller
import top.saymzx.scrcpy.server.helper.VideoEncode

object Server {
  // 连接流
  private lateinit var videoStream: BufferedSink
  private lateinit var audioStream: BufferedSink
  private lateinit var controlStreamIn: BufferedSource
  private lateinit var controlStreamOut: BufferedSink

  // 运行状态
  var isNormal = true

  // 主函数入口
  @JvmStatic
  fun main(args: Array<String>) {
    // 参数
    Options.parse(*args)
    // 执行线程
    val workThreads = ArrayList<Thread>()
    // 连接客户端
    connectClient()
    if (isNormal) {
      // 设置连接
      Device.controlStreamOut = controlStreamOut
      // 视频编码
      VideoEncode.videoStream = videoStream
      workThreads.add(VideoEncode.stream())
      // 音频编码
      AudioEncode.audioStream = audioStream
      val audioThreads = AudioEncode.stream()
      workThreads.add(audioThreads.first)
      workThreads.add(audioThreads.second)
      // 控制输入
      Controller.controlStreamIn = controlStreamIn
      workThreads.add(Controller.handle())
      // 启动
      for (workThread in workThreads) {
        workThread.start()
      }
      workThreads[0].join()
    }
    try {
      videoStream.close()
      audioStream.close()
      controlStreamIn.close()
      controlStreamOut.close()
    } catch (_: Exception) {
    }
    for (workThread in workThreads) {
      workThread.join()
    }
  }

  // 连接客户端
  private fun connectClient() {
    val serverSocket = LocalServerSocket("scrcpy_android")
    try {
      videoStream = serverSocket.accept().outputStream.sink().buffer()
      audioStream = serverSocket.accept().outputStream.sink().buffer()
      val controlSocket = serverSocket.accept()
      controlStreamIn = controlSocket.inputStream.source().buffer()
      controlStreamOut = controlSocket.outputStream.sink().buffer()
      serverSocket.close()
    } catch (_: Exception) {
      isNormal = false
      serverSocket.close()
    }
  }
}