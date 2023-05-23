package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.media.*
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardRequest
import com.malinskiy.adam.request.forwarding.PortForwardingMode
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import dadb.adbserver.AdbServer
import kotlinx.coroutines.*
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.*

class Scrcpy(val device: Device, private val main: MainActivity) {

  // 悬浮窗
  private val floatVideo = FloatVideo(main, this)

  // ip地址
  private val ip = Inet4Address.getByName(device.address).hostAddress!!

  // 被控端标识
  private var deviceID = "$ip:${device.port}"

  // 协程
  lateinit var coroutineScope: CoroutineScope

  // 控制队列
  val controls = LinkedList<ByteArray>() as Queue<ByteArray>

  // 是否支持音频
  private var canAudio = true

  // 音频播放器
  private lateinit var audioTrack: AudioTrack

  // 音频放大器
  private lateinit var loudnessEnhancer: LoudnessEnhancer

  // 连接流
  private lateinit var videoStream: DataInputStream
  private lateinit var audioStream: DataInputStream
  private lateinit var controlStream: DataOutputStream

  // 解码器
  private lateinit var videoDecodec: MediaCodec
  private lateinit var audioDecodec: MediaCodec

  // 开始投屏
  @SuppressLint("SourceLockedOrientationActivity")
  fun start() {

    Thread {
      runBlocking {
        coroutineScope = this
        var dialog: AlertDialog? = null
        try {
          device.status = 0
          // 显示加载中
          main.runOnUiThread {
            val text = TextView(main)
            text.text = "连接中"
            val builder: AlertDialog.Builder = AlertDialog.Builder(main)
            builder.setView(text)
            builder.setCancelable(false)
            val dia = builder.create()
            dia.setCanceledOnTouchOutside(false)
            dia.show()
            dialog = dia
          }
          // 发送server
          sendServer()
          // 连接server
          connectServer()
          // 配置视频解码
          setVideoDecodec()
          // 配置音频解码
          setAudioDecodec()
          // 初始化音频播放器
          if (canAudio) setAudioTrack()
          // 设置被控端熄屏（默认投屏后熄屏）
          setPowerOff()
          // 隐藏加载中
          main.runOnUiThread { dialog?.cancel() }
          // 显示悬浮窗
          floatVideo.show()
          device.status = 1
          // 创建解码线程
          launch { decodecInput(videoStream, videoDecodec) }
          launch { decodecOutput("video", videoDecodec) }
          // 创建解码线程
          if (canAudio) {
            launch { decodecInput(audioStream, audioDecodec) }
            launch { decodecOutput("audio", audioDecodec) }
          }
          // 配置控制输出
          launch { setControlOutput() }
        } catch (e: Exception) {
          Log.e("Scrcpy", e.toString())
          if (device.status == 1) floatVideo.hide()
          else {
            dialog?.cancel()
            stop()
          }
        }
      }
    }.start()
  }

  // 停止投屏
  fun stop() {
    // 收尾工作
    val job = coroutineScope.launch {
      // 恢复分辨率
      if (device.setResolution) runAdbCmd("wm size reset")
      // 删除旧进程
      runAdbCmd("ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9")
      // 断开连接
      main.appData.adb.execute(DisconnectDeviceRequest(ip, device.port))
    }
    while (job.isActive) {
    }
    coroutineScope.cancel()
    // 释放解码器
    videoDecodec.stop()
    videoDecodec.release()
    if (canAudio) {
      audioDecodec.stop()
      audioDecodec.release()
      loudnessEnhancer.release()
      audioTrack.stop()
      audioTrack.release()
    }
    main.runOnUiThread {
      Toast.makeText(main, "${device.name}:投屏已停止", Toast.LENGTH_SHORT).show()
    }
  }

  // 初始化音频播放器
  private fun setAudioTrack() {
    val sampleRate = 48000
    // 初始化音频播放器
    val minBufferSize = AudioTrack.getMinBufferSize(
      sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
    )
    audioTrack = AudioTrack.Builder().setAudioAttributes(
      AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(
      AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
    ).setBufferSizeInBytes(minBufferSize * 8).build()
    // 声音增强
    try {
      loudnessEnhancer = LoudnessEnhancer(audioTrack.audioSessionId)
      loudnessEnhancer.setTargetGain(4000)
      loudnessEnhancer.enabled = true
    } catch (_: IllegalArgumentException) {
    }
    audioTrack.play()
  }

  // 发送server
  private suspend fun sendServer() {
    // 连接ADB
    main.appData.adb.execute(ConnectDeviceRequest(ip, device.port))
    // 停止旧服务
    runAdbCmd("ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9")
    // 快速启动
    val versionCode = BuildConfig.VERSION_CODE
    if (runAdbCmd(" ls -l /data/local/tmp/scrcpy_server$versionCode.jar ").contains("No such file or directory")) {
      runAdbCmd(" rm /data/local/tmp/serverBase64 ")
      runAdbCmd("push ${main.applicationContext.filesDir.path}/scrcpy_server.jar /data/local/tmp/scrcpy_server$versionCode.jar")
    }
    runAdbCmd(" CLASSPATH=/data/local/tmp/scrcpy_server$versionCode.jar app_process / com.genymobile.scrcpy.Server 2.0 video_codec=${device.videoCodec} max_size=${device.maxSize} video_bit_rate=${device.videoBit} max_fps=${device.fps} > /dev/null 2>&1 & ")
    // 转发端口
    main.appData.adb.execute(
      PortForwardRequest(
        LocalTcpPortSpec(6007), RemoteTcpPortSpec(6006), deviceID, PortForwardingMode.NO_REBIND
      )
    )
  }

  // 连接server
  private suspend fun connectServer() {
    val videoSocket = Socket()
    val audioSocket = Socket()
    val controlSocket = Socket()
    // 尝试连接server,尝试5秒
    delay(5000)
    for (i in 1..100) {
      try {
        if (!videoSocket.isConnected) videoSocket.connect(InetSocketAddress("127.0.0.1", 6007))
        if (!audioSocket.isConnected) audioSocket.connect(InetSocketAddress("127.0.0.1", 6007))
        controlSocket.connect(InetSocketAddress("127.0.0.1", 6007))
        break
      } catch (_: Exception) {
        Log.e("scrcpy", "连接server失败，再次尝试")
        delay(50)
      }
    }
    // 连接失败
    if (!videoSocket.isConnected || !audioSocket.isConnected || !controlSocket.isConnected) {
      throw IOException("连接失败")
    }
    videoStream = DataInputStream(videoSocket.getInputStream())
    audioStream = DataInputStream(audioSocket.getInputStream())
    controlStream = DataOutputStream(controlSocket.getOutputStream())
  }

  // 视频解码器
  private fun setVideoDecodec() {
    // CodecMeta
    videoStream.readInt()
    floatVideo.remoteVideoWidth = videoStream.readInt()
    floatVideo.remoteVideoHeight = videoStream.readInt()
    // 创建解码器
    val codecMime =
      if (device.videoCodec == "h265") MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
    videoDecodec = MediaCodec.createDecoderByType(codecMime)
    val mediaFormat = MediaFormat.createVideoFormat(
      codecMime, floatVideo.remoteVideoWidth, floatVideo.remoteVideoHeight
    )
    // 获取视频标识头
    val csd0 = readFrame(videoStream)
    val csd1 = readFrame(videoStream)
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1))
    // 配置解码器
    videoDecodec.configure(
      mediaFormat,
      floatVideo.floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).holder.surface,
      null,
      0
    )
    // 启动解码器
    videoDecodec.start()
    // 解析首帧，解决开始黑屏问题
    var inIndex: Int
    do {
      inIndex = videoDecodec.dequeueInputBuffer(0)
    } while (inIndex < 0)
    videoDecodec.getInputBuffer(inIndex)!!.put(csd0)
    videoDecodec.queueInputBuffer(inIndex, 0, csd0.size, 0, 0)
    do {
      inIndex = videoDecodec.dequeueInputBuffer(0)
    } while (inIndex < 0)
    videoDecodec.getInputBuffer(inIndex)!!.put(csd1)
    videoDecodec.queueInputBuffer(inIndex, 0, csd1.size, 0, 0)
  }

  // 音频解码器
  private fun setAudioDecodec() {
    // 创建音频解码器
    audioDecodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
    // 是否不支持音频（安卓11以下不支持）
    if (audioStream.readInt() == 0) {
      canAudio = false
      return
    }
    // 音频参数
    val sampleRate = 48000
    val channelCount = 2
    val bitRate = 128000
    val mediaFormat =
      MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channelCount)
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
    // 获取音频标识头
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(readFrame(audioStream)))
    // csd1和csd2暂时没用到，所以默认全是用0
    val csd12bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val csd12ByteBuffer = ByteBuffer.wrap(csd12bytes, 0, csd12bytes.size)
    mediaFormat.setByteBuffer("csd-1", csd12ByteBuffer)
    mediaFormat.setByteBuffer("csd-2", csd12ByteBuffer)
    // 配置解码器
    audioDecodec.configure(mediaFormat, null, null, 0)
    // 启动解码器
    audioDecodec.start()
  }

  // 输入
  private fun decodecInput(stream: DataInputStream, decodec: MediaCodec) {
    var inIndex: Int
    // 开始解码
    while (coroutineScope.isActive) {
      // 向缓冲区输入数据帧
      val buffer = readFrame(stream)
      // 找到一个空的输入缓冲区
      do {
        inIndex = decodec.dequeueInputBuffer(0)
      } while (inIndex < 0)
      decodec.getInputBuffer(inIndex)!!.put(buffer)
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
    }
  }

  // 输出
  private suspend fun decodecOutput(mode: String, decodec: MediaCodec) {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    if (mode == "video") {
      var decodeNum = 0
      while (coroutineScope.isActive) {
        // 找到已完成的输出缓冲区
        outIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
        if (outIndex >= 0) {
          // 每两秒检查一次是否旋转，防止未收到旋转信息
          decodeNum++
          if (decodeNum > device.fps * 2 - 1) {
            decodeNum = 0
            floatVideo.isRotation(decodec.getOutputFormat(outIndex))
            checkScreenOff()
          }
          decodec.releaseOutputBuffer(outIndex, true)
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          floatVideo.isRotation(decodec.outputFormat)
        } else {
          delay(8)
          continue
        }
      }
    } else {
      while (coroutineScope.isActive) {
        // 找到已完成的输出缓冲区
        outIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
        if (outIndex < 0) {
          delay(4)
          continue
        }
        audioTrack.write(
          decodec.getOutputBuffer(outIndex)!!, bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING
        )
        decodec.releaseOutputBuffer(outIndex, false)
      }
    }
  }

  // 控制报文输出
  private suspend fun setControlOutput() {
    // 清除投屏之前的信息
    controls.clear()
    while (coroutineScope.isActive) {
      if (controls.isEmpty()) {
        delay(2)
        continue
      }
      try {
        val control = controls.poll()
        controlStream.write(control)
        controlStream.flush()
      } catch (_: NullPointerException) {
      }
    }
  }

  // 防止被控端熄屏
  private fun checkScreenOff() {
    coroutineScope.launch {
      if (!runAdbCmd("dumpsys deviceidle | grep mScreenOn").contains("mScreenOn=true")) runAdbCmd("input keyevent 26")
    }
  }

  // 被控端熄屏
  private fun setPowerOff() {
    val byteBuffer = ByteBuffer.allocate(2)
    byteBuffer.clear()
    byteBuffer.put(10)
    byteBuffer.put(0)
    byteBuffer.flip()
    controls.offer(byteBuffer.array())
  }

  // 从socket流中解析数据
  private fun readFrame(stream: DataInputStream): ByteArray {
    val size = stream.readInt()
    val buffer = ByteArray(size)
    stream.readFully(buffer, 0, size)
    return buffer
  }

  // 执行adb命令
  suspend fun runAdbCmd(cmd: String): String {
    return try {
      main.appData.adb.execute(ShellCommandRequest(cmd), deviceID).output
    } catch (e: Exception) {
      Log.e("Scrcpy", e.toString())
      throw (IOException("ADB命令失败"))
    }
  }

}