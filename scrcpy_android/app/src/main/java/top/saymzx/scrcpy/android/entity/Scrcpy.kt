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
import android.util.Base64
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
import top.saymzx.scrcpy.adb.AdbKeyPair
import top.saymzx.scrcpy.adb.AdbStream
import top.saymzx.scrcpy.android.MainActivity
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class Scrcpy(private val device: Device) {

  // ip地址
  private var ip = ""

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

  // 开始投屏
  private lateinit var alert: AlertDialog
  fun start() {
    device.isFull = defaultFull
    device.status = 0
    // 显示加载中
    alert = appData.publicTools.showLoading("连接中...", appData.main, true) {
      stop("用户停止")
    }
    appData.mainScope.launch {
      try {
        // 获取IP地址
        ip = withContext(Dispatchers.IO) {
          Inet4Address.getByName(device.address)
        }.hostAddress!!
        // 发送server
        sendServer()
        // 转发端口
        tcpForward()
      } catch (e: Exception) {
        stop("连接错误", e)
      }
      try {
        alert.cancel()
        // 配置视频解码
        setVideoDecodec()
        // 配置音频解码
        setAudioDecodec()
        // 配置音频播放
        if (canAudio) setAudioTrack()
        // 设置被控端熄屏（默认投屏后熄屏）
        setPowerOff()
      } catch (e: Exception) {
        stop("启动错误", e)
      }
      // 投屏中
      device.status = 1
      // 视频解码
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          decodeInput("video")
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          decodeVideoOutput()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      // 音频解码
      if (canAudio) {
        Thread {
          android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
          try {
            decodeInput("audio")
          } catch (e: Exception) {
            stop("投屏停止", e)
          }
        }.start()
      }
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          decodeAudioOutput()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      // 控制
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_LOWEST)
        try {
          setControlInput()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
      Thread {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_MORE_FAVORABLE)
        try {
          setControlOutput()
        } catch (e: Exception) {
          stop("投屏停止", e)
        }
      }.start()
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
      intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
      appData.main.startActivity(intent)
    }
  }

  // 发送server
  private suspend fun sendServer() {
    // 连接ADB
    withContext(Dispatchers.IO) {
      adb =
        Adb(ip, device.port, AdbKeyPair.read(appData.privateKey, appData.publicKey))
    }
    // 修改分辨率
    if (device.setResolution) {
      runAdbCmd(
        "wm size ${appData.deviceWidth}x${appData.deviceHeight}", false
      )
    }
    // 停止旧服务
    runAdbCmd(
      "ps -ef | grep scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9",
      false
    )
    // 快速启动
    val isHaveServer =
      runAdbCmd(" ls -l /data/local/tmp/scrcpy_server${appData.versionCode}.jar ", true)
    if (isHaveServer.contains("No such file or directory") || isHaveServer.contains("Invalid argument")) {
      runAdbCmd("rm /data/local/tmp/scrcpy_server* ", false)
      runAdbCmd("rm /data/local/tmp/scrcpy_server_base64 ", false)
      val serverFileBase64 = Base64.encode(withContext(Dispatchers.IO) {
        val server = appData.main.resources.openRawResource(R.raw.scrcpy_server)
        val buffer = ByteArray(server.available())
        server.read(buffer)
        server.close()
        buffer
      }, 2)
      var serverBase64part: String
      val len: Int = serverFileBase64.size
      var sourceOffset = 0
      while (sourceOffset < len) {
        if (len - sourceOffset >= 4056) {
          val filePart = ByteArray(4056)
          System.arraycopy(serverFileBase64!!, sourceOffset, filePart, 0, 4056)
          sourceOffset += 4056
          serverBase64part = String(filePart, StandardCharsets.US_ASCII)
        } else {
          val rem = len - sourceOffset
          val remPart = ByteArray(rem)
          System.arraycopy(serverFileBase64!!, sourceOffset, remPart, 0, rem)
          sourceOffset += rem
          serverBase64part = String(remPart, StandardCharsets.US_ASCII)
        }
        runAdbCmd(" echo $serverBase64part >> /data/local/tmp/scrcpy_server_base64", false)
      }
      delay(100)
      runAdbCmd(
        "base64 -d < /data/local/tmp/scrcpy_server_base64 > /data/local/tmp/scrcpy_server${appData.versionCode}.jar && rm /data/local/tmp/scrcpy_server_base64",
        true
      )
//      withContext(Dispatchers.IO) {
//        adb.pushFile(
//          appData.main.resources.openRawResource(R.raw.scrcpy_server),
//          "/data/local/tmp/scrcpy_server${appData.versionCode}.jar"
//        )
//      }
    }
    runAdbCmd(
      "CLASSPATH=/data/local/tmp/scrcpy_server${appData.versionCode}.jar app_process / com.genymobile.scrcpy.Server 2.1 video_codec=${device.videoCodec} audio_codec=${device.audioCodec} max_size=${device.maxSize} video_bit_rate=${device.videoBit} max_fps=${device.fps} > /dev/null 2>&1 & ",
      false
    )
  }

  // 转发端口
  private suspend fun tcpForward() {
    var connect = 0
    withContext(Dispatchers.IO) {
      for (i in 1..100) {
        if (device.status == -1) return@withContext
        try {
          if (connect == 0) {
            videoStream = adb.localSocketForward("scrcpy_android", true)
            connect = 1
          }
          if (connect == 1) {
            audioStream = adb.localSocketForward("scrcpy_android", true)
            connect = 2
          }
          controlStream = adb.localSocketForward("scrcpy_android", true)
          break
        } catch (_: Exception) {
          Log.i("Scrcpy", "连接失败，再次尝试")
          delay(100)
        }
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
          controls.write(it)
          synchronized(controls) {
            controls.notify()
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
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1))
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
    videoDecodec.getInputBuffer(inIndex)!!.put(csd0)
    videoDecodec.queueInputBuffer(inIndex, 0, csd0.size, 0, 0)
    inIndex = videoDecodec.dequeueInputBuffer(-1)
    videoDecodec.getInputBuffer(inIndex)!!.put(csd1)
    videoDecodec.queueInputBuffer(inIndex, 0, csd1.size, 0, 0)
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
      ByteBuffer.wrap(withContext(Dispatchers.IO) { readFrame(audioStream) })
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
  private fun decodeInput(mode: String) {
    val stream = if (mode == "video") videoStream else audioStream
    val codec = if (mode == "video") videoDecodec else audioDecodec
    var zeroFrameNum = 0
    // 开始解码
    while (device.status == 1) {
      val buffer = readFrame(stream)
      val inIndex = codec.dequeueInputBuffer(-1)
      if (inIndex >= 0) {
        codec.getInputBuffer(inIndex)!!.put(buffer)
        // 提交解码器解码
        codec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
        // 连续4个空包检测是否熄屏了
        if (buffer.size < 150) {
          zeroFrameNum++
          if (zeroFrameNum > 4) {
            zeroFrameNum = 0
            checkScreenOff()
          }
        }
      }
    }
  }

  // 视频解码输出
  private fun decodeVideoOutput() {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
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
      }
    }
  }

  // 音频解码输出
  private fun decodeAudioOutput() {
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
  private fun setControlInput() {
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
  private fun setControlOutput() {
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
          if (!runAdbCmd("dumpsys deviceidle | grep mScreenOn", true).contains("mScreenOn=true")) {
            isScreenOning = true
            runAdbCmd("input keyevent 26", false)
            delay(1000)
            setPowerOff()
            isScreenOning = false
          }
        } catch (_: Exception) {

        }
      }
    }
  }

  // 被控端熄屏
  private fun setPowerOff() {
    controls.write(byteArrayOf(10, 0))
    synchronized(controls) {
      controls.notify()
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
  private fun readFrame(stream: AdbStream): ByteArray {
    return try {
      stream.readByteArray(stream.readInt())
    } catch (_: IllegalStateException) {
      ByteArray(1)
    }
  }

  // 执行adb命令
  private suspend fun runAdbCmd(cmd: String, isNeedOutput: Boolean): String {
    return withContext(Dispatchers.IO) { adb.runAdbCmd(cmd, isNeedOutput) }
  }

}