package top.saymzx.easycontrol.server.helper;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Options;

public final class AudioEncode {
  public static MediaCodec encedec;
  public static final AudioRecord audioCapture = AudioCapture.start();

  public static Pair<Thread, Thread> stream() throws IOException {
    byte[] bytes = new byte[1];
    try {
      setAudioEncodec();
      encedec.start();
      Thread threadIn = new EncodeInThread();
      threadIn.setPriority(Thread.MAX_PRIORITY);
      Thread threadOut = new EncodeOutThread();
      threadOut.setPriority(Thread.MAX_PRIORITY);
      bytes[0] = 1;
      Server.writeFully(Server.audioStream, bytes, 0, 1);
      return new Pair<>(threadIn, threadOut);
    } catch (Exception e) {
      bytes[0] = 0;
      Server.writeFully(Server.audioStream, bytes, 0, 1);
      return new Pair<>(new Thread((Runnable) null), new Thread((Runnable) null));
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
        while (Server.isNormal) {
          inIndex = encedec.dequeueInputBuffer(-1);
          if (inIndex < 0) continue;
          len = audioCapture.read(encedec.getInputBuffer(inIndex), size);
          encedec.queueInputBuffer(inIndex, 0, len, 0, 0);
        }
      } catch (Exception ignored) {
        Server.isNormal = false;
      }
    }
  }

  static class EncodeOutThread extends Thread {
    @Override
    public void run() {
      try {
        int outIndex;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (Server.isNormal) {
          // 找到已完成的输出缓冲区
          outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1);
          if (outIndex < 0) continue;
          ByteBuffer byteBuffer = ByteBuffer.allocate(4);
          byteBuffer.putInt(bufferInfo.size);
          byteBuffer.flip();
          Server.writeFully(Server.audioStream, byteBuffer);
          Server.writeFully(Server.audioStream, encedec.getOutputBuffer(outIndex));
          encedec.releaseOutputBuffer(outIndex, false);
        }
      } catch (Exception ignored) {
        Server.isNormal = false;
      }
    }
  }
}

