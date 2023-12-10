package top.saymzx.easycontrol.app.client;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VideoDecode {

  private MediaCodec decodec;
  private MediaFormat decodecFormat;
  private final boolean isH265Support;
  private final Pair<byte[], Long> csd0;
  private final Pair<byte[], Long> csd1;

  public VideoDecode(Pair<Integer, Integer> videoSize, Pair<byte[], Long> csd0, Pair<byte[], Long> csd1) throws IOException {
    isH265Support = csd1 == null;
    this.csd0 = csd0;
    this.csd1 = csd1;
    setVideoDecodec(videoSize);
  }

  public void release() {
    try {
      decodec.stop();
      decodec.release();
    } catch (Exception ignored) {
    }
  }

  private final LinkedBlockingQueue<Integer> intputBufferQueue = new LinkedBlockingQueue<>();

  public boolean decodeIn(byte[] data, long pts) throws InterruptedException {
    try {
      Integer inIndex = intputBufferQueue.poll(20, TimeUnit.MILLISECONDS);
      if (inIndex == null) return false;
      decodec.getInputBuffer(inIndex).put(data);
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, data.length, pts, 0);
      return true;
    } catch (IllegalStateException ignored) {
      return false;
    }
  }

  // 创建Codec
  private void setVideoDecodec(Pair<Integer, Integer> videoSize) throws IOException {
    // 创建解码器
    String codecMime = isH265Support ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    decodecFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    decodecFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0.first));
    if (!isH265Support) decodecFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1.first));
    // 异步解码
    decodec.setCallback(new MediaCodec.Callback() {
      @Override
      public void onInputBufferAvailable(MediaCodec mediaCodec, int inIndex) {
        intputBufferQueue.offer(inIndex);
      }

      @Override
      public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
        try {
          decodec.releaseOutputBuffer(outIndex, bufferInfo.presentationTimeUs);
        } catch (IllegalStateException ignored) {
        }
      }

      @Override
      public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
      }

      @Override
      public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat format) {
      }
    });
  }

  public void setSurface(Surface surface) {
    // 配置解码器
    decodec.configure(decodecFormat, surface, null, 0);
    // 启动解码器
    decodec.start();
  }

  // 解析首帧，解决开始黑屏问题
  public void decodeInSpsPps() throws InterruptedException {
    for (int i = 0; i < 3; i++) if (decodeIn(csd0.first, csd0.second)) break;
    if (!isH265Support) for (int i = 0; i < 3; i++) if (decodeIn(csd1.first, csd1.second)) break;
  }

}
