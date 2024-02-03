package top.saymzx.easycontrol.app.client.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoDecode {
  private MediaCodec decodec;
  private final MediaCodec.Callback callback = new MediaCodec.Callback() {
    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inIndex) {
      intputBufferQueue.offer(inIndex);
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
      try {
        mediaCodec.releaseOutputBuffer(outIndex, bufferInfo.presentationTimeUs);
      } catch (IllegalStateException ignored) {
      }
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat format) {
    }
  };

  public VideoDecode(Pair<Integer, Integer> videoSize, Surface surface, ByteBuffer csd0, ByteBuffer csd1, Handler playHandler) throws IOException, InterruptedException {
    setVideoDecodec(videoSize, surface, csd0, csd1, playHandler);
  }

  public void release() {
    try {
      decodec.stop();
      decodec.release();
    } catch (Exception ignored) {
    }
  }

  private final LinkedBlockingQueue<Integer> intputBufferQueue = new LinkedBlockingQueue<>();

  public void decodeIn(ByteBuffer data) throws InterruptedException {
    long pts = data.getLong();
    int inIndex = intputBufferQueue.take();
    decodec.getInputBuffer(inIndex).put(data);
    decodec.queueInputBuffer(inIndex, 0, data.capacity() - 8, pts, 0);
  }

  // 创建Codec
  private void setVideoDecodec(Pair<Integer, Integer> videoSize, Surface surface, ByteBuffer csd0, ByteBuffer csd1, Handler playHandler) throws IOException, InterruptedException {
    boolean isH265Support = csd1 == null;
    csd0.position(8);
    // 创建解码器
    String codecMime = isH265Support ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    MediaFormat decodecFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    decodecFormat.setByteBuffer("csd-0", csd0);
    if (!isH265Support) {
      csd1.position(8);
      decodecFormat.setByteBuffer("csd-1", csd1);
    }
    // 异步解码
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playHandler != null) {
      decodec.setCallback(callback, playHandler);
    } else decodec.setCallback(callback);
    // 配置解码器
    decodec.configure(decodecFormat, surface, null, 0);
    // 启动解码器
    decodec.start();
    // 解析首帧，解决开始黑屏问题
    csd0.position(0);
    decodeIn(csd0);
    if (!isH265Support) {
      csd1.position(0);
      decodeIn(csd1);
    }
  }

}
