package top.saymzx.scrcpy.server.helper

import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.IBinder
import android.view.Surface
import okio.BufferedSink
import top.saymzx.scrcpy.server.Server.isNormal
import top.saymzx.scrcpy.server.entity.Device
import top.saymzx.scrcpy.server.entity.Options
import top.saymzx.scrcpy.server.wrappers.SurfaceControl

object VideoEncode {

  private lateinit var encedec: MediaCodec
  private lateinit var encodecFormat: MediaFormat
  lateinit var videoStream: BufferedSink

  private var isHasChangeRotation = false

  fun stream(): Thread {
    val thread = Thread {
      try {
        Device.rotationListener = {
          isHasChangeRotation = true
        }
        // 创建Codec
        setVideoEncodec()
        // 创建显示器
        val display = createDisplay()
        // 写入视频宽高
        videoStream.apply {
          writeInt(Device.videoSize.first)
          writeInt(Device.videoSize.second)
          flush()
        }
        // 旋转
        while (isNormal) {
          // 重配置编码器宽高
          encodecFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first)
          encodecFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second)
          encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
          // 绑定Display和Surface
          setDisplaySurface(
            display,
            encedec.createInputSurface(),
            Device.layerStack,
            Device.deviceSize,
            Device.videoSize
          )
          // 启动编码
          encedec.start()
          encode()
          encedec.stop()
        }
      } catch (e: Exception) {
        print(e)
        isNormal = false
        try {
          encedec.stop()
          encedec.release()
        } catch (_: Exception) {
        }
      }
    }
    thread.priority = Thread.MAX_PRIORITY
    return thread
  }


  // 创建Codec
  private fun setVideoEncodec() {
    val codecMime =
      if (Options.videoCodec == "h265") MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
    encedec = MediaCodec.createEncoderByType(codecMime)
    encodecFormat = MediaFormat()
    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime)
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, Options.videoBitRate)
    encodecFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60)
    encodecFormat.setInteger(
      MediaFormat.KEY_COLOR_FORMAT,
      MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    )
    encodecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)
    encodecFormat.setLong(
      MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
      100000.toLong() // 100ms
    )
    encodecFormat.setFloat("max-fps-to-encoder", Options.maxFps.toFloat())
  }

  // 创建显示器
  private fun createDisplay(): IBinder {
    val secure =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.R && "S" != Build.VERSION.CODENAME
    return SurfaceControl.createDisplay("scrcpy_android", secure)
  }

  // 绑定Surface和Dispaly
  private fun setDisplaySurface(
    display: IBinder,
    surface: Surface,
    layerStack: Int,
    DeviceSize: Pair<Int, Int>,
    videoSize: Pair<Int, Int>
  ) {
    SurfaceControl.openTransaction()
    try {
      SurfaceControl.setDisplaySurface(display, surface)
      SurfaceControl.setDisplayProjection(
        display,
        -1,
        Rect(0, 0, DeviceSize.first, DeviceSize.second),
        Rect(0, 0, videoSize.first, videoSize.second)
      )
      SurfaceControl.setDisplayLayerStack(display, layerStack)
    } finally {
      SurfaceControl.closeTransaction()
    }
  }

  // 编码
  private fun encode() {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    while (!isHasChangeRotation && isNormal) {
      // 找到已完成的输出缓冲区
      outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1)
      if (outIndex < 0) continue
      val buffer = encedec.getOutputBuffer(outIndex)
      videoStream.apply {
        writeInt(bufferInfo.size)
        writeLong(System.currentTimeMillis())
        write(buffer)
        flush()
      }
      encedec.releaseOutputBuffer(outIndex, false)
    }
    isHasChangeRotation = false
  }

}