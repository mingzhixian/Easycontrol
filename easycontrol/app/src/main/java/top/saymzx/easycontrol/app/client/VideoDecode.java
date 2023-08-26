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
  public Boolean videoPortrait;

  public VideoDecode(Client c) throws IOException {
    client = c;
    // 读取画面大小
    readVideoSize();
    // 创建Codec
    setVideoDecodec();
  }

  public Pair<Thread, Thread> start() {
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
        while (client.isNormal.get()) {
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
        client.stop("连接中断", null);
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
        while (client.isNormal.get()) {
          // 找到已完成的输出缓冲区
          outIndex = videoDecodec.dequeueOutputBuffer(bufferInfo, -1);
          if (outIndex < 0) continue;
          videoDecodec.releaseOutputBuffer(outIndex, true);
          fps++;
        }
      } catch (Exception ignored) {
        client.stop("连接中断", null);
      }
    }
  }

  // 读取画面大小
  private void readVideoSize() {
    int remoteVideoWidth = client.videoStream.readInt();
    int remoteVideoHeight = client.videoStream.readInt();
    videoPortrait = remoteVideoHeight > remoteVideoWidth;
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
  private Boolean has60 = false;

  private Pair<Integer, byte[]> readFrame() {
    int size = client.videoStream.readInt();
    int sendMs = client.videoStream.readInt();
    int isKeyFrame = client.videoStream.readByte();
    byte[] frame = client.videoStream.readByteArray(size);
    // 丢帧处理
    int oldDelay = delays[currentIndex];
    int nowMs = (int) (System.currentTimeMillis() & 0x00000000FFFFFFFFL);
    int nowDelay = nowMs - sendMs;
    // 取正，两个设备时间可能相差较大
    nowDelay = (nowDelay + (nowDelay >> 31)) ^ (nowDelay >> 31);
    delays[currentIndex] = nowDelay;
    avgDelay = avgDelay - oldDelay + nowDelay;
    currentIndex = (currentIndex + 1) % 60;
    // 大于1.25倍的平均延迟则丢帧，只丢弃非关键帧
    if (has60) if (isKeyFrame == 0 && nowDelay > (avgDelay / 48)) return null;
    else if (currentIndex == 59) has60 = true;
    return new Pair<>(size, frame);
  }
}
