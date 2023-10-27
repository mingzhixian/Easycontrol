package top.saymzx.easycontrol.app.client;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.LoudnessEnhancer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecode {
  public MediaCodec audioDecodec;
  public AudioTrack audioTrack;
  public LoudnessEnhancer loudnessEnhancer;

  public AudioDecode(ByteBuffer csd0) throws IOException {
    // 创建Codec
    setAudioDecodec(csd0);
    // 创建AudioTrack
    setAudioTrack();
    // 创建音频放大器
    setLoudnessEnhancer();
  }

  public void release() {
    try {
      audioTrack.stop();
      audioTrack.release();
      loudnessEnhancer.release();
      audioDecodec.stop();
      audioDecodec.release();
    } catch (Exception ignored) {
    }
  }

  public synchronized void decodeIn(ByteBuffer data) {
    try {
      int inIndex = audioDecodec.dequeueInputBuffer(0);
      // 缓冲区已满丢帧
      if (inIndex < 0) return;
      int size = data.remaining();
      audioDecodec.getInputBuffer(inIndex).put(data);
      // 提交解码器解码
      audioDecodec.queueInputBuffer(inIndex, 0, size, 0, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public void decodeOut(boolean isNormalPlay) {
    try {
      int outIndex = audioDecodec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex >= 0) {
        if (isNormalPlay)
          audioTrack.write(audioDecodec.getOutputBuffer(outIndex), bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING);
        audioDecodec.releaseOutputBuffer(outIndex, false);
      }
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  private void setAudioDecodec(ByteBuffer csd0) throws IOException {
    // 创建解码器
    String codecMime = MediaFormat.MIMETYPE_AUDIO_OPUS;
    audioDecodec = MediaCodec.createDecoderByType(codecMime);
    // 音频参数
    int sampleRate = 48000;
    int channelCount = 2;
    int bitRate = 64000;
    MediaFormat mediaFormat = MediaFormat.createAudioFormat(codecMime, sampleRate, channelCount);
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    // 获取音频标识头
    mediaFormat.setByteBuffer("csd-0", csd0);
    // csd1和csd2暂时没用到，所以默认全是用0
    ByteBuffer csd12ByteBuffer = ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
    mediaFormat.setByteBuffer("csd-1", csd12ByteBuffer);
    mediaFormat.setByteBuffer("csd-2", csd12ByteBuffer);
    // 配置解码器
    audioDecodec.configure(mediaFormat, null, null, 0);
    // 启动解码器
    audioDecodec.start();
  }

  // 创建AudioTrack
  private void setAudioTrack() {
    AudioTrack.Builder audioDecodecBuild = new AudioTrack.Builder();
    int sampleRate = 48000;
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
}
