package top.saymzx.easycontrol.app.client;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

import kotlin.Pair;

public class VideoDecode {
  private final Client client;
  public MediaCodec videoDecodec;
  public Pair<Integer, Integer> videoSize;
  public Boolean devicePortrait;

  public VideoDecode(Client c) throws IOException {
    client = c;
    // 读取画面大小
    readVideoSize();
    // 创建Codec
    setVideoDecodec();
  }

  public Pair<Thread, Thread> stream() {
    Thread streamInThread = new StreamInThread();
    Thread streamOutThread = new StreamOutThread();
    streamInThread.setPriority(Thread.MAX_PRIORITY);
    streamOutThread.setPriority(Thread.MAX_PRIORITY);
    return new Pair<>(streamInThread, streamOutThread);
  }

  class StreamInThread extends Thread {
    @Override
    public void run() {
      try {
        // 开始解码
        int inIndex;
        while (client.isNormal) {
          Pair<Integer, byte[]> frame = readFrame();
          if (frame != null) {
            inIndex = videoDecodec.dequeueInputBuffer(-1);
            if (inIndex >= 0) {
              videoDecodec.getInputBuffer(inIndex).put(frame.getSecond());
              // 提交解码器解码
              videoDecodec.queueInputBuffer(inIndex, 0, frame.getFirst(), 0, 0);
            }
          }
        }
      } catch (Exception ignored) {
        client.isNormal = false;
      }
    }
  }

  public int fps = 0;

  class StreamOutThread extends Thread {
    @Override
    public void run() {
      try {
        int outIndex;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (client.isNormal) {
          // 找到已完成的输出缓冲区
          outIndex = videoDecodec.dequeueOutputBuffer(bufferInfo, -1);
          if (outIndex >= 0) {
            videoDecodec.releaseOutputBuffer(outIndex, true);
            fps++;
          }
        }
      } catch (Exception ignored) {
        client.isNormal = false;
      }
    }
  }

  // 读取画面大小
  private void readVideoSize() {
    int remoteVideoWidth = client.videoStream.readInt();
    int remoteVideoHeight = client.videoStream.readInt();
    devicePortrait = remoteVideoHeight > remoteVideoWidth;
    videoSize = new Pair<>(remoteVideoWidth, remoteVideoHeight);
  }

  // 创建Codec
  private void setVideoDecodec() throws IOException {
    // 创建解码器
    String codecMime;
    if (client.device.getVideoCodec().equals("h265")) codecMime = MediaFormat.MIMETYPE_VIDEO_HEVC;
    else codecMime = MediaFormat.MIMETYPE_VIDEO_AVC;
    videoDecodec = MediaCodec.createDecoderByType(codecMime);
    MediaFormat mediaFormat = MediaFormat.createVideoFormat(codecMime, videoSize.getFirst(), videoSize.getSecond());
    // 获取视频标识头
    Pair<Integer, byte[]> csd0 = readFrame();
    Pair<Integer, byte[]> csd1 = readFrame();
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0.getSecond()));
    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1.getSecond()));
    // 配置低延迟解码
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
      if (videoDecodec.getCodecInfo().getCapabilitiesForType(codecMime).isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency))
        mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
    // 配置解码器
    videoDecodec.configure(mediaFormat, client.surface, null, 0);
    // 启动解码器
    videoDecodec.start();
    // 解析首帧，解决开始黑屏问题
    int inIndex = videoDecodec.dequeueInputBuffer(-1);
    videoDecodec.getInputBuffer(inIndex).put(csd0.getSecond());
    videoDecodec.queueInputBuffer(inIndex, 0, csd0.getFirst(), 0, 0);
    inIndex = videoDecodec.dequeueInputBuffer(-1);
    videoDecodec.getInputBuffer(inIndex).put(csd1.getSecond());
    videoDecodec.queueInputBuffer(inIndex, 0, csd0.getFirst(), 0, 0);
  }

  // 从socket流中解析数据
  private final int[] delays = new int[60];
  private int currentIndex = 0;
  public int avgDelay = 0;

  private Pair<Integer, byte[]> readFrame() {
    int size = client.videoStream.readInt();
    long sendMs = client.videoStream.readLong();
    byte[] frame = client.videoStream.readByteArray(size);
    // 丢帧处理
    int noeDelay = (int) (System.currentTimeMillis() - sendMs);
    int oldestDelay = delays[currentIndex];
    delays[currentIndex] = noeDelay;
    avgDelay = avgDelay - oldestDelay + noeDelay;
    currentIndex = (currentIndex + 1) % 60;
    if (noeDelay > (avgDelay / 30) && delays[59] != 0)// 大于2倍的平均延迟则丢帧
      return null;
    return new Pair<>(size, frame);
  }
}
