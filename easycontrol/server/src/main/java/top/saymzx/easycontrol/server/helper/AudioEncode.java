/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Options;

public final class AudioEncode {
  private static MediaCodec encedec;
  private static AudioRecord audioCapture;
  private static boolean useOpus;

  public static boolean init() throws IOException {
    useOpus = Options.supportOpus && EncodecTools.isSupportOpus();
    byte[] bytes = new byte[]{0};
    try {
      // 从安卓12开始支持音频
      if (!Options.isAudio || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw new Exception("版本低");
      setAudioEncodec();
      encedec.start();
      audioCapture = AudioCapture.init();
    } catch (Exception ignored) {
      Server.writeMain(ByteBuffer.wrap(bytes));
      return false;
    }
    bytes[0] = 1;
    Server.writeMain(ByteBuffer.wrap(bytes));
    bytes[0] = (byte) (useOpus ? 1 : 0);
    Server.writeMain(ByteBuffer.wrap(bytes));
    return true;
  }

  private static void setAudioEncodec() throws IOException {
    String codecMime = useOpus ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC;
    encedec = MediaCodec.createEncoderByType(codecMime);
    MediaFormat encodecFormat = MediaFormat.createAudioFormat(codecMime, AudioCapture.SAMPLE_RATE, AudioCapture.CHANNELS);
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
    encodecFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioCapture.AUDIO_PACKET_SIZE);
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
  }

  public static void encodeIn() {
    try {
      int inIndex;
      do inIndex = encedec.dequeueInputBuffer(-1); while (inIndex < 0);
      ByteBuffer buffer = encedec.getInputBuffer(inIndex);
      if (buffer == null) return;
      int size = Math.min(buffer.remaining(), AudioCapture.AUDIO_PACKET_SIZE);
      audioCapture.read(buffer, size);
      encedec.queueInputBuffer(inIndex, 0, size, 0, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private static final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public static void encodeOut() throws IOException {
    try {
      // 找到已完成的输出缓冲区
      int outIndex;
      do outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1); while (outIndex < 0);
      ByteBuffer buffer = encedec.getOutputBuffer(outIndex);
      if (buffer == null) return;
      if (useOpus) {
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          buffer.getLong();
          int size = (int) buffer.getLong();
          buffer.limit(buffer.position() + size);
        }
        // 当无声音时不发送
        if (buffer.remaining() < 5) {
          encedec.releaseOutputBuffer(outIndex, false);
          return;
        }
      }
      ControlPacket.sendAudioEvent(buffer);
      encedec.releaseOutputBuffer(outIndex, false);
    } catch (IllegalStateException ignored) {
    }
  }

  public static void release() {
    try {
      encedec.stop();
      encedec.release();
      audioCapture.stop();
      audioCapture.release();
    } catch (Exception ignored) {
    }
  }
}

