/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.system.ErrnoException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import top.saymzx.easycontrol.server.Server;

public final class AudioEncode {
  public static MediaCodec encedec;
  public static AudioRecord audioCapture;

  public static boolean init() throws IOException, ErrnoException {
    byte[] bytes = new byte[1];
    try {
      // 从安卓12开始支持
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw new Exception("版本低");
      setAudioEncodec();
      encedec.start();
      audioCapture = AudioCapture.init();
    } catch (Exception ignored) {
      Server.write(ByteBuffer.wrap(bytes));
      return false;
    }
    bytes[0] = 1;
    Server.write(ByteBuffer.wrap(bytes));
    encodeIn();
    encodeOut();
    return true;
  }

  private static void setAudioEncodec() throws IOException {
    String codecMime = MediaFormat.MIMETYPE_AUDIO_OPUS;
    encedec = MediaCodec.createEncoderByType(codecMime);
    MediaFormat encodecFormat = new MediaFormat();
    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime);
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
    encodecFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AudioCapture.CHANNELS);
    encodecFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AudioCapture.SAMPLE_RATE);
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
  }

  private static final int frameSize = AudioCapture.millisToBytes(10);

  public static void encodeIn() {
    try {
      int inIndex;
      do inIndex = encedec.dequeueInputBuffer(-1); while (inIndex < 0);
      audioCapture.read(encedec.getInputBuffer(inIndex), frameSize);
      encedec.queueInputBuffer(inIndex, 0, frameSize, 0, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private static final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public static void encodeOut() throws InterruptedIOException, ErrnoException {
    try {
      // 找到已完成的输出缓冲区
      int outIndex;
      do outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1); while (outIndex < 0);
      ByteBuffer buffer = encedec.getOutputBuffer(outIndex);
      if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
        buffer.getLong();
        int size = (int) buffer.getLong();
        buffer.limit(buffer.position() + size);
      }
      // 当无声音时不发送
      int frameSize = buffer.remaining();
      if (frameSize > 5) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(5 + frameSize);
        byteBuffer.put((byte) 2);
        byteBuffer.putInt(frameSize);
        byteBuffer.put(buffer);
        byteBuffer.flip();
        Server.write(byteBuffer);
      }
      encedec.releaseOutputBuffer(outIndex, false);
    } catch (IllegalStateException ignored) {
    }
  }
}

