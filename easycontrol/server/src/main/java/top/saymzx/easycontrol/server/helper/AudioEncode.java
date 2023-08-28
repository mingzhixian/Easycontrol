/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Options;

public final class AudioEncode {
  public static MediaCodec encedec;
  public static AudioRecord audioCapture;

  public static Pair<Thread, Thread> start() {
    byte[] bytes = new byte[1];
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      try {
        Server.writeFully(Server.audioStream, bytes);
      } catch (IOException ignored) {
        Server.isNormal.set(false);
      }
      return null;
    }
    try {
      audioCapture = AudioCapture.start();
      setAudioEncodec();
      encedec.start();
      Thread threadIn = new EncodeInThread();
      threadIn.setPriority(Thread.MAX_PRIORITY);
      Thread threadOut = new EncodeOutThread();
      threadOut.setPriority(Thread.MAX_PRIORITY);
      bytes[0] = 1;
      Server.writeFully(Server.audioStream, bytes);
      return new Pair<>(threadIn, threadOut);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static void setAudioEncodec() throws IOException {
    String codecMime = Objects.equals(Options.audioCodec, "opus") ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC;
    encedec = MediaCodec.createEncoderByType(codecMime);
    MediaFormat encodecFormat = new MediaFormat();
    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime);
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
    encodecFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AudioCapture.CHANNELS);
    encodecFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AudioCapture.SAMPLE_RATE);
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
  }

  static class EncodeInThread extends Thread {
    @Override
    public void run() {
      try {
        int size = AudioCapture.millisToBytes(5);
        int len;
        int inIndex;
        while (Server.isNormal.get()) {
          inIndex = encedec.dequeueInputBuffer(-1);
          if (inIndex < 0) continue;
          len = audioCapture.read(encedec.getInputBuffer(inIndex), size);
          encedec.queueInputBuffer(inIndex, 0, len, 0, 0);
        }
      } catch (Exception ignored) {
        Server.isNormal.set(false);
      }
    }
  }

  static class EncodeOutThread extends Thread {
    @Override
    public void run() {
      try {
        int outIndex;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (Server.isNormal.get()) {
          // 找到已完成的输出缓冲区
          outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1);
          if (outIndex < 0) continue;
          ByteBuffer buffer = encedec.getOutputBuffer(outIndex);
          if (Objects.equals(Options.audioCodec, "opus") && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
            fixOpusConfigPacket(buffer);
          ByteBuffer byteBuffer = ByteBuffer.allocate(4);
          byteBuffer.putInt(buffer.remaining());
          byteBuffer.flip();
          Server.writeFully(Server.audioStream, byteBuffer);
          Server.writeFully(Server.audioStream, buffer);
          encedec.releaseOutputBuffer(outIndex, false);
        }
      } catch (Exception ignored) {
        Server.isNormal.set(false);
      }
    }
  }

  private static final long AOPUSHDR = 0x5244485355504F41L; // "AOPUSHDR" in ASCII (little-endian)

  private static void fixOpusConfigPacket(ByteBuffer buffer) throws IOException {
    // Here is an example of the config packet received for an OPUS stream:
    //
    // 00000000  41 4f 50 55 53 48 44 52  13 00 00 00 00 00 00 00  |AOPUSHDR........|
    // -------------- BELOW IS THE PART WE MUST PUT AS EXTRADATA  -------------------
    // 00000010  4f 70 75 73 48 65 61 64  01 01 38 01 80 bb 00 00  |OpusHead..8.....|
    // 00000020  00 00 00                                          |...             |
    // ------------------------------------------------------------------------------
    // 00000020           41 4f 50 55 53  44 4c 59 08 00 00 00 00  |   AOPUSDLY.....|
    // 00000030  00 00 00 a0 2e 63 00 00  00 00 00 41 4f 50 55 53  |.....c.....AOPUS|
    // 00000040  50 52 4c 08 00 00 00 00  00 00 00 00 b4 c4 04 00  |PRL.............|
    // 00000050  00 00 00                                          |...|
    //
    // Each "section" is prefixed by a 64-bit ID and a 64-bit length.
    //
    // <https://developer.android.com/reference/android/media/MediaCodec#CSD>

    if (buffer.remaining() < 16) {
      throw new IOException("Not enough data in OPUS config packet");
    }

    long id = buffer.getLong();
    if (id != AOPUSHDR) {
      throw new IOException("OPUS header not found");
    }

    long sizeLong = buffer.getLong();
    if (sizeLong < 0 || sizeLong >= 0x7FFFFFFF) {
      throw new IOException("Invalid block size in OPUS header: " + sizeLong);
    }

    int size = (int) sizeLong;
    if (buffer.remaining() < size) {
      throw new IOException("Not enough data in OPUS header (invalid size: " + size + ")");
    }

    // Set the buffer to point to the OPUS header slice
    buffer.limit(buffer.position() + size);
  }
}

