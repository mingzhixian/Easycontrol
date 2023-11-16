package top.saymzx.easycontrol.app.client;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecode {

  private MediaCodec decodec;
  private MediaFormat decodecFormat;
  private final boolean isH265Support;

  public VideoDecode(Pair<Integer, Integer> videoSize, Pair<Long, ByteBuffer> csd0, Pair<Long, ByteBuffer> csd1) throws IOException {
    isH265Support = csd1 == null;
    setVideoDecodec(videoSize, csd0, csd1);
  }

  public void release() {
    try {
      decodec.stop();
      decodec.release();
    } catch (Exception ignored) {
    }
  }

  public void decodeIn(Pair<Long, ByteBuffer> data) {
    try {
      int inIndex = decodec.dequeueInputBuffer(20_000);
      // 缓冲区已满则丢帧
      if (inIndex < 0) return;
      decodec.getInputBuffer(inIndex).put(data.second);
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, data.second.capacity(), data.first, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public void decodeOut(boolean isNormalPlay) {
    try {
      int outIndex = decodec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex >= 0) decodec.releaseOutputBuffer(outIndex, isNormalPlay);
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  private void setVideoDecodec(Pair<Integer, Integer> videoSize, Pair<Long, ByteBuffer> csd0, Pair<Long, ByteBuffer> csd1) throws IOException {
    // 创建解码器
    String codecMime = isH265Support ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    decodecFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    decodecFormat.setByteBuffer("csd-0", csd0.second);
    if (!isH265Support) decodecFormat.setByteBuffer("csd-1", csd1.second);
    decodecFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
  }

  public void setSurface(Surface surface) {
    // 配置解码器
    decodec.configure(decodecFormat, surface, null, 0);
    // 启动解码器
    decodec.start();
    // 解析首帧，解决开始黑屏问题
    decodeIn(new Pair<>(0L, decodecFormat.getByteBuffer("csd-0")));
    if (!isH265Support) decodeIn(new Pair<>(0L, decodecFormat.getByteBuffer("csd-1")));
  }

}
