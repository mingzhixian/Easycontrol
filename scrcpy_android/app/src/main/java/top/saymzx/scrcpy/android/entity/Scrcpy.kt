package top.saymzx.scrcpy.android.entity

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.WRITE_NON_BLOCKING
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Process.THREAD_PRIORITY_LOWEST
import android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notify
import okhttp3.internal.wait
import okio.Buffer
import top.saymzx.scrcpy.adb.Adb
import top.saymzx.scrcpy.adb.AdbStream
import top.saymzx.scrcpy.android.MainActivity
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class Scrcpy(private val device: Device) {

  // 视频悬浮窗
  private lateinit var floatVideo: FloatVideo

  // 音频
  private var canAudio = true
  private lateinit var audioTrack: AudioTrack
  private lateinit var loudnessEnhancer: LoudnessEnhancer

  // 连接流
  private lateinit var adb: Adb
  private lateinit var videoStream: AdbStream
  private lateinit var audioStream: AdbStream
  private lateinit var controlStream: AdbStream

  // 解码器
  private lateinit var videoDecodec: MediaCodec
  private lateinit var audioDecodec: MediaCodec

  // 剪切板
  private var clipBoardText = ""

  // 刷新率
  private var fps = 0

  // 开始投屏
  private lateinit var alert: AlertDialog
  fun start() {
    device.isFull = appData.setValue.defaultFull
    device.status = 0
    // 显示加载中
    alert = appData.publicTools.showLoading("连接中...", appData.main, true) {
      stop("用户停止")
    }
    appData.mainScope.launch {
      // 连接ADB
      connectADB()
      // 发送server
      sendServer()
      // 启动server
      startServer()
      // 连接server
      connectServer()
      alert.cancel()
      try {
        // 配置视频解码
        setVideoDecodec()
        // 配置音频解码
        setAudioDecodec()
        // 配置音频播放
        if (canAudio) setAudioTrack()
      } catch (e: Exception) {
        stop("启动错误", e)
      }
      // 投屏中
      if (device.status < 0) return@launch
      device.status = 1
      // 设置被控端熄屏
      setPowerOff()
      // 视频解码
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          decodeInputThread("video")
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          decodeVideoOutputThread()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      // 音频解码
      if (canAudio) {
        Thread {
          android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
          try {
            decodeInputThread("audio")
          } catch (e: Exception) {
            stop("投屏停止", e)
          }
        }.start()
        Thread {
          android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
          try {
            decodeAudioOutputThread()
          } catch (e: Exception) {
            stop("投屏停止", e)
          }
        }.start()
      }
      // 控制
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_LOWEST)
        try {
          controlInputThread()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          controlOutputThread()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      launch {
        // 显示刷新率
        if (appData.setValue.showFps) floatVideo.floatVideo.floatVideoFps.text = "0"
        while (device.status == 1) {
          delay(1000)
          // 显示刷新率
          if (appData.setValue.showFps) {
            floatVideo.floatVideo.floatVideoFps.text = fps.toString()
            fps = 0
          }
          // 熄屏检测
          checkScreenOff()
        }
      }
    }
  }

  // 停止投屏
  private val stopSetStatus = Object()
  fun stop(scrcpyError: String, e: Exception? = null) {
    // 防止多次调用
    synchronized(stopSetStatus) {
      if (device.status == -1) return
      device.status = -1
    }
    appData.isFocus = false
    appData.mainScope.launch {
      withContext(Dispatchers.Main) {
        Toast.makeText(appData.main, scrcpyError, Toast.LENGTH_SHORT).show()
        if (e != null)
          Toast.makeText(appData.main, "详细信息：$e", Toast.LENGTH_SHORT).show()
        Log.e("Scrcpy", "$scrcpyError---${e?.toString() ?: ""}")
      }
      // 恢复分辨率
      try {
        if (device.setResolution) runAdbCmd("wm size reset", false)
        runAdbCmd(
          "ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9",
          false
        )
        adb.close()
      } catch (_: Exception) {
      }
    }
    try {
      alert.cancel()
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
      controlStream.close()
    } catch (_: Exception) {
    }
    try {
      floatVideo.hide()
    } catch (_: Exception) {
    }
    device.scrcpy = null
    if (device.isFull) {
      val intent = Intent(appData.main, MainActivity::class.java)
      intent.putExtra("startDefault", false)
      intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
      appData.main.startActivity(intent)
    }
  }

  // 连接ADB
  private suspend fun connectADB() {
    val ip = try {
      // 获取IP地址
      withContext(Dispatchers.IO) {
        Inet4Address.getByName(device.address)
      }.hostAddress!!
    } catch (e: Exception) {
      stop("解析域名失败", e)
      return
    }
    try {
      // 连接ADB
      withContext(Dispatchers.IO) {
        adb = Adb(ip, device.port, appData.keyPair)
      }
    } catch (e: Exception) {
      stop("连接ADB失败", e)
      return
    }
  }

  // 发送server
  private suspend fun sendServer() {
    if (device.status < 0) return
    // 尝试3次
    var isHaveServer =
      runAdbCmd(" ls -l /data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar ", true)
    for (i in 0..2) {
      if (isHaveServer.contains("No such file or directory") || isHaveServer.contains("Invalid argument")) {
        runAdbCmd("rm /data/local/tmp/scrcpy_android_server_* ", false)
        withContext(Dispatchers.IO) {
          adb.pushFile(
            appData.main.resources.openRawResource(R.raw.scrcpy_server),
            "/data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar"
          )
        }
        // 检查是否传送完成
        isHaveServer =
          runAdbCmd(
            " ls -l /data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar ",
            true
          )
      } else {
        return
      }
    }
    stop("发送Server失败")
  }

  // 启动Server
  private suspend fun startServer() {
    if (device.status < 0) return
    try {
      // 修改分辨率
      if (device.setResolution) {
        runAdbCmd(
          "wm size ${appData.deviceWidth}x${appData.deviceHeight}", false
        )
      }
      // 停止旧服务
      runAdbCmd(
        "ps -ef | grep scrcpy_android_server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9",
        false
      )
      // 启动Server
      runAdbCmd(
        "CLASSPATH=/data/local/tmp/scrcpy_android_server_${appData.versionCode}.jar app_process / com.genymobile.scrcpy.Server 2.1 video_codec=${device.videoCodec} audio_codec=${device.audioCodec} max_size=${device.maxSize} video_bit_rate=${device.videoBit} max_fps=${device.fps} > /dev/null 2>&1 & ",
        false
      )
    } catch (e: Exception) {
      stop("ADB连接中断", e)
      return
    }
  }

  // 连接Server
  private suspend fun connectServer() {
    if (device.status < 0) return
    var connect = 0
    withContext(Dispatchers.IO) {
      for (i in 1..100) {
        try {
          if (connect == 0) {
            videoStream = adb.tcpForward(6007, true)
            connect = 1
          }
          if (connect == 1) {
            audioStream = adb.tcpForward(6007, true)
            connect = 2
          }
          controlStream = adb.tcpForward(6007, true)
          connect = 3
          break
        } catch (_: Exception) {
          delay(100)
        }
      }
      if (connect != 3) {
        stop("连接Server失败", null)
        return@withContext
      }
    }
  }

  // 视频解码器
  private var checkRotationNotification = false
  private suspend fun setVideoDecodec() {
    // CodecMeta
    withContext(Dispatchers.IO) {
      videoStream.readInt()
      val remoteVideoWidth = videoStream.readInt()
      val remoteVideoHeight = videoStream.readInt()
      // 显示悬浮窗
      floatVideo =
        FloatVideo(device, remoteVideoWidth, remoteVideoHeight) {
          try {
            controls.write(it)
            synchronized(controls) {
              controls.notify()
            }
          } catch (e: Exception) {
            stop("连接断开", e)
          }
        }
    }
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
    val csd0 = withContext(Dispatchers.IO) { readFrame(videoStream) }
    val csd1 = withContext(Dispatchers.IO) { readFrame(videoStream) }
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0.third))
    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1.third))
    // 配置低延迟解码
    val codeInfo = videoDecodec.codecInfo
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (codeInfo.getCapabilitiesForType(codecMime)
          .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)
      ) mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
    }
    // 配置解码器
    videoDecodec.configure(
      mediaFormat,
      floatVideo.floatVideo.floatVideoSurface.holder.surface,
      null,
      0
    )
    // 启动解码器
    videoDecodec.start()
    // 解析首帧，解决开始黑屏问题
    var inIndex = videoDecodec.dequeueInputBuffer(-1)
    videoDecodec.getInputBuffer(inIndex)!!.put(csd0.third)
    videoDecodec.queueInputBuffer(inIndex, 0, csd0.second, csd0.first, 0)
    inIndex = videoDecodec.dequeueInputBuffer(-1)
    videoDecodec.getInputBuffer(inIndex)!!.put(csd1.third)
    videoDecodec.queueInputBuffer(inIndex, 0, csd1.second, csd1.first, 0)
  }

  // 音频解码器
  private suspend fun setAudioDecodec() {
    // 创建音频解码器
    val codecMime =
      if (device.audioCodec == "opus") MediaFormat.MIMETYPE_AUDIO_OPUS else MediaFormat.MIMETYPE_AUDIO_AAC
    audioDecodec = MediaCodec.createDecoderByType(codecMime)
    // 是否不支持音频（安卓11以下不支持）
    val can = withContext(Dispatchers.IO) { audioStream.readInt() }
    if (can == 0) {
      canAudio = false
      return
    }
    // 音频参数
    val sampleRate = 48000
    val channelCount = 2
    val bitRate = 64000
    val mediaFormat =
      MediaFormat.createAudioFormat(codecMime, sampleRate, channelCount)
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
    // 获取音频标识头
    mediaFormat.setByteBuffer(
      "csd-0",
      ByteBuffer.wrap(withContext(Dispatchers.IO) { readFrame(audioStream).third })
    )
    if (device.audioCodec == "opus") {
      // csd1和csd2暂时没用到，所以默认全是用0
      val csd12bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
      val csd12ByteBuffer = ByteBuffer.wrap(csd12bytes, 0, csd12bytes.size)
      mediaFormat.setByteBuffer("csd-1", csd12ByteBuffer)
      mediaFormat.setByteBuffer("csd-2", csd12ByteBuffer)
    }
    // 配置解码器
    audioDecodec.configure(mediaFormat, null, null, 0)
    // 启动解码器
    audioDecodec.start()
  }

  // 初始化音频播放器
  private fun setAudioTrack() {
    val audioDecodecBuild = AudioTrack.Builder()
    val sampleRate = 44100
    val minBufferSize = AudioTrack.getMinBufferSize(
      sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
    )
    audioDecodecBuild.setBufferSizeInBytes(minBufferSize * 4)
    val audioAttributesBulider = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    audioDecodecBuild.setAudioAttributes(audioAttributesBulider.build())
    audioDecodecBuild.setAudioFormat(
      AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
    )
    audioTrack = audioDecodecBuild.build()
    // 声音增强
    try {
      loudnessEnhancer = LoudnessEnhancer(audioTrack.audioSessionId)
      loudnessEnhancer.setTargetGain(3000)
      loudnessEnhancer.enabled = true
    } catch (_: Exception) {
      Toast.makeText(appData.main, "音频放大器未生效", Toast.LENGTH_SHORT).show()
    }
    audioTrack.play()
  }

  // 输入
  private fun decodeInputThread(mode: String) {
    val stream = if (mode == "video") videoStream else audioStream
    val codec = if (mode == "video") videoDecodec else audioDecodec
    // 开始解码
    while (device.status == 1) {
      val buffer = readFrame(stream)
      val inIndex = codec.dequeueInputBuffer(-1)
      if (inIndex >= 0) {
        codec.getInputBuffer(inIndex)!!.put(buffer.third)
        // 提交解码器解码
        codec.queueInputBuffer(inIndex, 0, buffer.second, buffer.first, 0)
      }
    }
  }

  // 视频解码输出
  private fun decodeVideoOutputThread() {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    val showFps = appData.setValue.showFps
    while (device.status == 1) {
      // 找到已完成的输出缓冲区
      outIndex = videoDecodec.dequeueOutputBuffer(bufferInfo, -1)
      if (outIndex >= 0) {
        // 是否需要检查旋转(仍需要检查，因为可能是90°和270°的旋转)
        if (checkRotationNotification) {
          checkRotationNotification = false
          val width = videoDecodec.getOutputFormat(outIndex).getInteger("width")
          val height = videoDecodec.getOutputFormat(outIndex).getInteger("height")
          appData.mainScope.launch {
            withContext(Dispatchers.Main) {
              floatVideo.checkRotation(width, height)
            }
          }
        }
        videoDecodec.releaseOutputBuffer(outIndex, true)
        if (showFps) fps++
      }
    }
  }

  // 音频解码输出
  private fun decodeAudioOutputThread() {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    var loopNum = 0
    while (device.status == 1) {
      loopNum++
      if (loopNum > 200) {
        loopNum = 0
        checkClipBoard()
      }
      // 找到已完成的输出缓冲区
      outIndex = audioDecodec.dequeueOutputBuffer(bufferInfo, 0)
      if (outIndex >= 0) {
        audioTrack.write(
          audioDecodec.getOutputBuffer(outIndex)!!,
          bufferInfo.size,
          WRITE_NON_BLOCKING
        )
        audioDecodec.releaseOutputBuffer(outIndex, false)
      } else if (outIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        Thread.sleep(4)
        continue
      }
    }
  }

  // 检测报文输入
  private fun controlInputThread() {
    while (device.status == 1) {
      // 检测被控端剪切板变化
      val type =
        try {
          controlStream.readByte().toInt()
        } catch (_: IllegalStateException) {
          -1
        }
      when (type) {
        // 剪切板报告报文
        0 -> {
          val newClipBoardText = String(
            controlStream.readByteArray(
              controlStream.readInt()
            ),
            StandardCharsets.UTF_8
          )
          if (clipBoardText != newClipBoardText) {
            clipBoardText = newClipBoardText
            appData.clipBorad.setPrimaryClip(
              ClipData.newPlainText(
                MIMETYPE_TEXT_PLAIN,
                clipBoardText
              )
            )
          }
        }
        // 设置剪切板回应报文
        1 -> {
          controlStream.readLong()
        }
        // 设置旋转报文
        2 -> {
          // 延迟0.5秒等待画面稳定
          Thread.sleep(500)
          checkRotationNotification = true
        }
      }
    }
  }

  // 控制报文输出
  private val controls = Buffer()
  private fun controlOutputThread() {
    while (device.status == 1) {
      if (!controls.request(1)) synchronized(controls) {
        controls.wait()
      }
      controlStream.write(controls.readByteArray())
    }
  }

  // 防止被控端熄屏
  private var isScreenOning = false
  private fun checkScreenOff() {
    // 避免短时重复操作
    if (!isScreenOning) {
      appData.mainScope.launch {
        try {
          isScreenOning = true
          if (!runAdbCmd("dumpsys deviceidle | grep mScreenOn", true).contains("mScreenOn=true")) {
            runAdbCmd("input keyevent 26", false)
            delay(1000)
            setPowerOff()
            delay(1000)
          }
          isScreenOning = false
        } catch (_: Exception) {
        }
      }
    }
  }

  // 被控端熄屏
  private fun setPowerOff() {
    if (appData.setValue.slaveTurnOffScreen) {
      controls.write(byteArrayOf(10, 0))
      synchronized(controls) {
        controls.notify()
      }
    }
  }

  // 同步本机剪切板至被控端
  private fun checkClipBoard() {
    val clipBorad = appData.clipBorad.primaryClip
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
    controls.write(byteBuffer)
    synchronized(controls) {
      controls.notify()
    }
  }

  // 从socket流中解析数据
  private fun readFrame(stream: AdbStream): Triple<Long, Int, ByteArray> {
    return try {
      val pts = stream.readLong()
      val size = stream.readInt()
      val frame = stream.readByteArray(size)
      return Triple(pts, size, frame)
    } catch (_: IllegalStateException) {
      Triple(System.currentTimeMillis() * 1000, 1, ByteArray(1))
    }
  }

  // 执行adb命令
  private suspend fun runAdbCmd(cmd: String, isNeedOutput: Boolean): String {
    return withContext(Dispatchers.IO) {
      try {
        adb.runAdbCmd(cmd, isNeedOutput)
      } catch (_: Exception) {
        ""
      }
    }
  }

}