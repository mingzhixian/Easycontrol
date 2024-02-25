package top.saymzx.easycontrol.app.client.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
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
    try {
      long pts = data.getLong();
      int inIndex = intputBufferQueue.take();
      decodec.getInputBuffer(inIndex).put(data);
      decodec.queueInputBuffer(inIndex, 0, data.capacity() - 8, pts, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  private void setVideoDecodec(Pair<Integer, Integer> videoSize, Surface surface, ByteBuffer csd0, ByteBuffer csd1, Handler playHandler) throws IOException, InterruptedException {
    boolean useH265 = csd1 == null;
    // 创建解码器
    String codecMime = useH265 ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    try {
      String codecName = DecodecTools.getVideoDecoder(useH265);
      if (Objects.equals(codecName, "")) decodec = MediaCodec.createDecoderByType(codecMime);
      else decodec = MediaCodec.createByCodecName(codecName);
    } catch (Exception ignord) {
      decodec = MediaCodec.createDecoderByType(codecMime);
    }
    MediaFormat decodecFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    csd0.position(8);
    decodecFormat.setByteBuffer("csd-0", csd0);
    if (!useH265) {
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
    if (!useH265) {
      csd1.position(0);
      decodeIn(csd1);
    }
  }

}
