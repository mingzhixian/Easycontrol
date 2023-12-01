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

  public void decodeIn(byte[] data, long pts) {
    try {
      int inIndex = decodec.dequeueInputBuffer(20_000);
      // 缓冲区已满则丢帧
      if (inIndex < 0) return;
      decodec.getInputBuffer(inIndex).put(data);
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, data.length, pts, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public void decodeOut(boolean isNormalPlay) {
    try {
      int outIndex = decodec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex >= 0) {
        if (!isNormalPlay) decodec.releaseOutputBuffer(outIndex, false);
        else decodec.releaseOutputBuffer(outIndex, bufferInfo.presentationTimeUs);
      }
    } catch (IllegalStateException ignored) {
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
  }

  public void setSurface(Surface surface) {
    // 配置解码器
    decodec.configure(decodecFormat, surface, null, 0);
    // 启动解码器
    decodec.start();
    // 解析首帧，解决开始黑屏问题
    decodeIn(csd0.first, csd0.second);
    if (!isH265Support) decodeIn(csd1.first, csd0.second);
  }

}
