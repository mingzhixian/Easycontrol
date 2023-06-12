package top.saymzx.scrcpy.android

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.LinkedList
import java.util.Queue

class Scrcpy(val device: Device, val main: MainActivity) {

  // ip地址
  private var ip = ""

  // 协程
  var mainScope = MainScope()

  // 视频悬浮窗
  private lateinit var floatVideo: FloatVideo

  // 音频
  private var canAudio = true
  private lateinit var audioTrack: AudioTrack
  private lateinit var loudnessEnhancer: LoudnessEnhancer

  // 连接流
  private lateinit var adb: Dadb
  private lateinit var videoStream: BufferedSource
  private lateinit var audioStream: BufferedSource
  private lateinit var controlOutStream: BufferedSink
  private lateinit var controlInStream: BufferedSource

  // 输出锁
  private val controlOutputLock = Mutex()

  // 解码器
  private lateinit var videoDecodec: MediaCodec
  private lateinit var audioDecodec: MediaCodec

  // 剪切板
  private var clipBoardText = ""

  // 开始投屏
  fun start() {
    device.isFull = device.defaultFull
    device.status = 0
    // 显示加载中
    main.appData.showLoading("连接中...", true) {
      stop()
    }
    main.appData.loadingDialog.show()
    mainScope.launch {
      try {
        // 获取IP地址
        ip = withContext(Dispatchers.IO) {
          Inet4Address.getByName(device.address)
        }.hostAddress!!
        // 发送server
        sendServer()
        // 转发端口
        tcpForward()
        // 配置视频解码
        setVideoDecodec()
        // 配置音频解码
        setAudioDecodec()
        // 配置音频播放
        if (canAudio) setAudioTrack()
        // 视频解码
        launch { decodeInput("video") }
        launch { decodeOutput("video") }
        // 音频解码
        if (canAudio) {
          launch { decodeInput("audio") }
          launch { decodeOutput("audio") }
        }
        // 配置控制
        launch { setControlInput() }
        // 投屏中
        device.status = 1
        // 设置被控端熄屏（默认投屏后熄屏）
        setPowerOff()
      } catch (e: Exception) {
        if (device.status != -1) {
          Toast.makeText(main, e.toString(), Toast.LENGTH_SHORT).show()
          Log.e("Scrcpy", e.toString())
          stop()
        }
      }
    }
  }

  // 停止投屏
  fun stop() {
    if (device.status == 0) main.appData.loadingDialog.cancel()
    device.status = -1
    try {
      // 恢复分辨率
      if (device.setResolution) mainScope.launch { runAdbCmd("wm size reset") }
      mainScope.launch {
        runAdbCmd("ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9")
      }
    } catch (_: Exception) {
    }
    try {
      floatVideo.hide()
    } catch (_: Exception) {
    }
    try {
      if (canAudio) {
        loudnessEnhancer.release()
        audioTrack.stop()
        audioTrack.release()
        audioDecodec.stop()
        audioDecodec.release()
      }
    } catch (_: Exception) {
    }
    try {
      videoDecodec.stop()
      videoDecodec.release()
    } catch (_: Exception) {
    }
    try {
      videoStream.close()
      audioStream.close()
      controlOutStream.close()
      controlInStream.close()
      adb.close()
    } catch (_: Exception) {
    }
    mainScope.cancel()
  }

  // 初始化音频播放器
  private fun setAudioTrack() {
    val audioDecodecBuild = AudioTrack.Builder()
    val sampleRate = 48000
    val minBufferSize = AudioTrack.getMinBufferSize(
      sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
    )
    audioDecodecBuild.setBufferSizeInBytes(minBufferSize * 4)
    val audioAttributesBulider = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    if (!device.setLoud) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioDecodecBuild.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        audioAttributesBulider.setFlags(AudioAttributes.FLAG_LOW_LATENCY)
      }
    }
    audioDecodecBuild.setAudioAttributes(audioAttributesBulider.build())
    audioDecodecBuild.setAudioFormat(
      AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
    )
    audioTrack = audioDecodecBuild.build()
    // 声音增强
    if (device.setLoud) {
      try {
        loudnessEnhancer = LoudnessEnhancer(audioTrack.audioSessionId)
        loudnessEnhancer.setTargetGain(4000)
        loudnessEnhancer.enabled = true
      } catch (_: Exception) {
        Toast.makeText(main, "音频放大器未生效", Toast.LENGTH_SHORT).show()
      }
    }
    audioTrack.play()
  }

  // 发送server
  private suspend fun sendServer() {
    // 连接ADB
    adb =
      Dadb.create(ip, device.port, AdbKeyPair.read(main.appData.privateKey, main.appData.publicKey))
    // 修改分辨率
    if (device.setResolution) runAdbCmd(
      "wm size ${main.appData.deviceWidth}x${main.appData.deviceHeight}"
    )
    // 停止旧服务
    runAdbCmd("ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9")
    // 快速启动
    if (runAdbCmd(" ls -l /data/local/tmp/scrcpy_server${main.appData.versionCode}.jar ").contains("No such file or directory")) {
      runAdbCmd("rm /data/local/tmp/serverBase64")
      runAdbCmd("rm /data/local/tmp/scrcpy_server*")
      val serverFileBase64 = Base64.encodeToString(withContext(Dispatchers.IO) {
        val server = main.resources.openRawResource(R.raw.scrcpy_server)
        val buffer = ByteArray(server.available())
        server.read(buffer)
        server.close()
        buffer
      }, 2)
      runAdbCmd("echo $serverFileBase64 >> /data/local/tmp/serverBase64\n")
      runAdbCmd("base64 -d < /data/local/tmp/serverBase64 > /data/local/tmp/scrcpy_server${main.appData.versionCode}.jar && rm /data/local/tmp/serverBase64")
    }
    runAdbCmd("CLASSPATH=/data/local/tmp/scrcpy_server${main.appData.versionCode}.jar app_process / com.genymobile.scrcpy.Server 2.0 video_codec=${device.videoCodec} max_size=${device.maxSize} video_bit_rate=${device.videoBit} max_fps=${device.fps} > /dev/null 2>&1 &")
  }

  // 转发端口
  private suspend fun tcpForward() {
    var connect = 0
    withContext(Dispatchers.IO) {
      for (i in 1..100) {
        try {
          if (connect == 0) {
            videoStream = adb.open("tcp:6006").source
            connect = 1
          }
          if (connect == 1) {
            audioStream = adb.open("tcp:6006").source
            connect = 2
          }
          val control = adb.open("tcp:6006")
          controlOutStream = control.sink
          controlInStream = control.source
          break
        } catch (_: Exception) {
          Log.i("Scrcpy", "连接失败，再次尝试")
          delay(50)
        }
      }
    }
  }

  // 视频解码器
  private suspend fun setVideoDecodec() {
    // CodecMeta
    withContext(Dispatchers.IO) {
      videoStream.readInt()
      val remoteVideoWidth = videoStream.readInt()
      val remoteVideoHeight = videoStream.readInt()
      // 显示悬浮窗
      floatVideo = FloatVideo(this@Scrcpy, remoteVideoWidth, remoteVideoHeight)
    }
    main.appData.loadingDialog.cancel()
    floatVideo.show()
    // 创建解码器
    val codecMime =
      if (device.videoCodec == "h265") MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
    videoDecodec = MediaCodec.createDecoderByType(codecMime)
    val mediaFormat = MediaFormat.createVideoFormat(
      codecMime,
      floatVideo.remoteVideoWidth,
      floatVideo.remoteVideoHeight
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
  private suspend fun setAudioDecodec() {
    // 创建音频解码器
    audioDecodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
    // 是否不支持音频（安卓11以下不支持）
    val can = withContext(Dispatchers.IO) { audioStream.readInt() }
    if (can == 0) {
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
  private suspend fun decodeInput(mode: String) {
    val stream = if (mode == "video") videoStream else audioStream
    val decodec = if (mode == "video") videoDecodec else audioDecodec
    var inIndex = -1
    val isCheckScreen = mode == "video"
    var zeroFrameNum = 0
    // 开始解码
    while (mainScope.isActive) {
      // 向缓冲区输入数据帧
      val buffer = readFrame(stream)
      // 连续4个空包检测是否熄屏了
      if (isCheckScreen) {
        if (buffer.size < 150) {
          zeroFrameNum++
          if (zeroFrameNum > 4) {
            zeroFrameNum = 0
            checkScreenOff()
          }
        }
      }
      // 找到一个空的输入缓冲区
      while (mainScope.isActive) {
        inIndex = decodec.dequeueInputBuffer(0)
        if (inIndex >= 0) break
        delay(4)
      }
      decodec.getInputBuffer(inIndex)!!.put(buffer)
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
    }
  }

  // 输出
  private var checkRotationNotification = false
  private suspend fun decodeOutput(mode: String) {
    val decodec = if (mode == "video") videoDecodec else audioDecodec
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    if (mode == "video") {
      while (mainScope.isActive) {
        // 找到已完成的输出缓冲区
        outIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
        if (outIndex >= 0) {
          // 是否需要检查旋转
          if (checkRotationNotification) {
            checkRotationNotification = false
            floatVideo.checkRotation(
              decodec.getOutputFormat(outIndex).getInteger("width"),
              decodec.getOutputFormat(outIndex).getInteger("height")
            )
          }
          decodec.releaseOutputBuffer(outIndex, true)
        } else {
          delay(4)
          continue
        }
      }
    } else {
      var loopNum = 0
      while (mainScope.isActive) {
        loopNum++
        if (loopNum > 150) {
          loopNum = 0
          checkClipBoard()
        }
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

  // 检测报文输入
  private suspend fun setControlInput() {
    while (mainScope.isActive) {
      // 检测被控端剪切板变化
      val type = withContext(Dispatchers.IO) {
        try {
          controlInStream.readByte().toInt()
        } catch (_: IllegalStateException) {
          -1
        }
      }
      when (type) {
        // 剪切板报告报文
        0 -> {
          val newClipBoardText = String(
            withContext(Dispatchers.IO) {
              controlInStream.readByteArray(
                controlInStream.readInt().toLong()
              )
            },
            StandardCharsets.UTF_8
          )
          if (clipBoardText != newClipBoardText) {
            clipBoardText = newClipBoardText
            main.appData.clipBorad.setPrimaryClip(
              ClipData.newPlainText(
                MIMETYPE_TEXT_PLAIN,
                clipBoardText
              )
            )
          }
        }
        // 设置剪切板回应报文
        1 -> {
          withContext(Dispatchers.IO) {
            controlInStream.readLong()
          }
        }
        // 设置旋转报文
        2 -> {
          // 延迟0.5秒等待画面稳定
          delay(500)
          checkRotationNotification = true
        }
      }
    }
  }

  // 防止被控端熄屏
  private var isScreenOning = false
  private fun checkScreenOff() {
    mainScope.launch {
      if (!runAdbCmd("dumpsys deviceidle | grep mScreenOn").contains("mScreenOn=true") && !isScreenOning) {
        // 避免短时重复操作
        isScreenOning = true
        runAdbCmd("input keyevent 26")
        delay(100)
        isScreenOning = false
      }
    }
  }

  // 被控端熄屏
  private fun setPowerOff() {
    writeControlOutput(byteArrayOf(10, 0))
  }

  // 同步本机剪切板至被控端
  private fun checkClipBoard() {
    val clipBorad = main.appData.clipBorad.primaryClip
    val newClipBoardText =
      if (clipBorad != null && clipBorad.itemCount > 0) clipBorad.getItemAt(0).text.toString() else ""
    if (clipBoardText != newClipBoardText && newClipBoardText != "") {
      clipBoardText = newClipBoardText
      setClipBoard(clipBoardText)
    }
  }

  // 设置剪切板文本
  private fun setClipBoard(text: String) {
    val textByteArray = text.toByteArray(StandardCharsets.UTF_8)
    val byteBuffer = ByteBuffer.allocate(14 + textByteArray.size)
    byteBuffer.clear()
    byteBuffer.put(9)
    byteBuffer.putLong(101)
    byteBuffer.put(0)
    byteBuffer.putInt(textByteArray.size)
    byteBuffer.put(textByteArray)
    byteBuffer.flip()
    writeControlOutput(byteBuffer.array())
  }

  // 从socket流中解析数据
  private suspend fun readFrame(stream: BufferedSource): ByteArray {
    return withContext(Dispatchers.IO) {
      try {
        stream.readByteArray(stream.readInt().toLong())
      } catch (_: IllegalStateException) {
        ByteArray(1)
      }
    }
  }

  // 执行adb命令
  suspend fun runAdbCmd(cmd: String): String {
    return withContext(Dispatchers.IO) { adb.shell(cmd).allOutput }
  }

  // 控制报文输出
  private val controls = LinkedList<ByteArray>() as Queue<ByteArray>
  private var isWriting = false
  fun writeControlOutput(byteArray: ByteArray) {
    if (device.status == 1) {
      // 减少大量协程开销
      controls.offer(byteArray)
      if (!isWriting) {
        mainScope.launch {
          isWriting = true
          withContext(Dispatchers.IO) {
            controlOutputLock.withLock {
              while (!controls.isEmpty()) {
                try {
                  controls.poll()?.let {
                    controlOutStream.write(it)
                    controlOutStream.flush()
                  }
                } catch (_: NullPointerException) {
                }
              }
            }
          }
          isWriting = false
        }
      }
    }
  }

}