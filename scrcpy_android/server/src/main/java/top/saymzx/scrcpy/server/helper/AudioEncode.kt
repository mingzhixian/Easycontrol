package top.saymzx.scrcpy.server.helper

import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import okio.BufferedSink
import top.saymzx.scrcpy.server.Server.isNormal
import top.saymzx.scrcpy.server.entity.Options

object AudioEncode {

  private lateinit var encedec: MediaCodec
  private lateinit var encodecFormat: MediaFormat
  private lateinit var audioCapture: AudioRecord
  lateinit var audioStream: BufferedSink

  fun stream(): Pair<Thread, Thread> {
    try {
      audioCapture = AudioCapture.start()
      setAudioEncodec()
      encedec.start()
    } catch (e: Exception) {
      print(e)
      isNormal = false
      try {
        audioCapture.stop()
        audioCapture.release()
        encedec.stop()
        encedec.release()
        audioStream.writeInt(0)
        return Pair(Thread {}, Thread {})
      } catch (_: Exception) {
      }
    }
    val threadIn = Thread {
      try {
        encodeIn()
      } catch (e: Exception) {
        print(e)
        isNormal = false
        try {
          audioCapture.stop()
          audioCapture.release()
          encedec.stop()
          encedec.release()
        } catch (_: Exception) {
        }
      }
    }
    threadIn.priority = Thread.MAX_PRIORITY
    val threadOut = Thread {
      try {
        encodeOut()
      } catch (e: Exception) {
        print(e)
        isNormal = false
        try {
          audioCapture.stop()
          audioCapture.release()
          encedec.stop()
          encedec.release()
        } catch (_: Exception) {
        }
      }
    }
    threadOut.priority = Thread.MAX_PRIORITY
    audioStream.writeInt(1)
    return Pair(threadIn, threadOut)
  }

  // 创建Codec
  private fun setAudioEncodec() {
    val codecMime =
      if (Options.audioCodec == "opus") MediaFormat.MIMETYPE_AUDIO_OPUS else MediaFormat.MIMETYPE_AUDIO_AAC
    encedec = MediaCodec.createEncoderByType(codecMime)
    encodecFormat = MediaFormat()
    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime)
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
    encodecFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AudioCapture.CHANNELS)
    encodecFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AudioCapture.SAMPLE_RATE)
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
  }

  // 编码入
  private fun encodeIn() {
    val size = AudioCapture.millisToBytes(5)
    var len: Int
    while (isNormal) {
      val inIndex = encedec.dequeueInputBuffer(-1)
      if (inIndex < 0) continue
      len = audioCapture.read(encedec.getInputBuffer(inIndex)!!, size)
      encedec.queueInputBuffer(inIndex, 0, len, 0, 0)
    }
  }

  // 编码出
  private fun encodeOut() {
    var outIndex: Int
    val bufferInfo = MediaCodec.BufferInfo()
    while (isNormal) {
      // 找到已完成的输出缓冲区
      outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1)
      if (outIndex < 0) continue
      val buffer = encedec.getOutputBuffer(outIndex)
      audioStream.apply {
        writeInt(bufferInfo.size)
        writeLong(System.currentTimeMillis())
        write(buffer)
        flush()
      }
      encedec.releaseOutputBuffer(outIndex, false)
    }
  }
}