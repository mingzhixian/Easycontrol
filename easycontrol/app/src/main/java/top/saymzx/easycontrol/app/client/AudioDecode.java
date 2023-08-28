package top.saymzx.easycontrol.app.client;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.LoudnessEnhancer;

import java.io.IOException;
import java.nio.ByteBuffer;

import kotlin.Pair;

public class AudioDecode {
  private final Client client;
  public MediaCodec audioDecodec;
  public AudioTrack audioTrack;
  public LoudnessEnhancer loudnessEnhancer;
  private boolean canAudio = true;

  public AudioDecode(Client c) throws IOException {
    client = c;
    // 读取是否支持音频
    if (client.audioStream.readByte() == 1) {
      // 创建Codec
      setAudioDecodec();
      // 创建AudioTrack
      setAudioTrack();
      // 创建音频放大器
      setLoudnessEnhancer();
    } else canAudio = false;
  }

  public Pair<Thread, Thread> start() {
    if (canAudio) {
      Thread streamInThread = new StreamInThread();
      Thread streamOutThread = new StreamOutThread();
      streamInThread.setPriority(Thread.MAX_PRIORITY);
      streamOutThread.setPriority(Thread.MAX_PRIORITY);
      return new Pair<>(streamInThread, streamOutThread);
    } else return null;
  }

  class StreamInThread extends Thread {
    @Override
    public void run() {
      try {
        // 开始解码
        int inIndex;
        while (client.isNormal.get()) {
          Pair<Integer, byte[]> frame = readFrame();
          inIndex = audioDecodec.dequeueInputBuffer(-1);
          if (inIndex < 0) continue;
          audioDecodec.getInputBuffer(inIndex).put(frame.getSecond());
          // 提交解码器解码
          audioDecodec.queueInputBuffer(inIndex, 0, frame.getFirst(), 0, 0);
        }
      } catch (Exception ignored) {
        client.stop("连接中断", null);
      }
    }
  }

  class StreamOutThread extends Thread {
    @Override
    public void run() {
      try {
        int outIndex;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (client.isNormal.get()) {
          // 找到已完成的输出缓冲区
          outIndex = audioDecodec.dequeueOutputBuffer(bufferInfo, -1);
          if (outIndex < 0) continue;
          audioTrack.write(audioDecodec.getOutputBuffer(outIndex), bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING);
          audioDecodec.releaseOutputBuffer(outIndex, false);
        }
      } catch (Exception ignored) {
        client.stop("连接中断", null);
      }
    }
  }

  // 创建Codec
  private void setAudioDecodec() throws IOException {
    // 创建解码器
    String codecMime;
    if (client.device.getAudioCodec().equals("opus")) codecMime = MediaFormat.MIMETYPE_AUDIO_OPUS;
    else codecMime = MediaFormat.MIMETYPE_AUDIO_AAC;
    audioDecodec = MediaCodec.createDecoderByType(codecMime);
    // 音频参数
    int sampleRate = 48000;
    int channelCount = 2;
    int bitRate = 64000;
    MediaFormat mediaFormat = MediaFormat.createAudioFormat(codecMime, sampleRate, channelCount);
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    // 获取音频标识头
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(readFrame().getSecond()));
    if (client.device.getAudioCodec().equals("opus")) {
      // csd1和csd2暂时没用到，所以默认全是用0
      byte[] csd12bytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
      ByteBuffer csd12ByteBuffer = ByteBuffer.wrap(csd12bytes, 0, csd12bytes.length);
      mediaFormat.setByteBuffer("csd-1", csd12ByteBuffer);
      mediaFormat.setByteBuffer("csd-2", csd12ByteBuffer);
    }
    // 配置解码器
    audioDecodec.configure(mediaFormat, null, null, 0);
    // 启动解码器
    audioDecodec.start();
  }

  // 创建AudioTrack
  private void setAudioTrack() {
    AudioTrack.Builder audioDecodecBuild = new AudioTrack.Builder();
    int sampleRate = 44100;
    // 1
    AudioAttributes.Builder audioAttributesBulider = new AudioAttributes.Builder();
    audioAttributesBulider.setUsage(AudioAttributes.USAGE_MEDIA);
    audioAttributesBulider.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
    // 2
    AudioFormat.Builder audioFormat = new AudioFormat.Builder();
    audioFormat.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
    audioFormat.setSampleRate(sampleRate);
    audioFormat.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
    // 3
    audioDecodecBuild.setBufferSizeInBytes(AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 4);
    audioDecodecBuild.setAudioAttributes(audioAttributesBulider.build());
    audioDecodecBuild.setAudioFormat(audioFormat.build());
    // 4
    audioTrack = audioDecodecBuild.build();
    audioTrack.play();
  }

  // 创建音频放大器
  private void setLoudnessEnhancer() {
    loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
    loudnessEnhancer.setTargetGain(3500);
    loudnessEnhancer.setEnabled(true);
  }

  // 从socket流中解析数据
  private Pair<Integer, byte[]> readFrame() {
    int size = client.audioStream.readInt();
    byte[] frame = client.audioStream.readByteArray(size);
    return new Pair<>(size, frame);
  }
}
