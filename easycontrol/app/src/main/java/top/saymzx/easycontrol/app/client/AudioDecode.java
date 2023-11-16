package top.saymzx.easycontrol.app.client;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecode {
  public MediaCodec decodec;
  public AudioTrack audioTrack;
  public LoudnessEnhancer loudnessEnhancer;

  public AudioDecode(Pair<Long, ByteBuffer> csd0) throws IOException {
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
      decodec.stop();
      decodec.release();
    } catch (Exception ignored) {
    }
  }

  public void decodeIn(Pair<Long, ByteBuffer> data) {
    try {
      int inIndex = decodec.dequeueInputBuffer(0);
      // 缓冲区已满丢帧
      if (inIndex < 0) return;
      decodec.getInputBuffer(inIndex).put(data.second);
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, data.second.capacity(), data.first, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public void decodeOut() {
    try {
      int outIndex = decodec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex >= 0) {
        audioTrack.write(decodec.getOutputBuffer(outIndex), bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING);
        decodec.releaseOutputBuffer(outIndex, false);
      }
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  private void setAudioDecodec(Pair<Long, ByteBuffer> csd0) throws IOException {
    // 创建解码器
    String codecMime = MediaFormat.MIMETYPE_AUDIO_AAC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    // 音频参数
    int sampleRate = 48000;
    int channelCount = 2;
    int bitRate = 96000;
    MediaFormat decodecFormat = MediaFormat.createAudioFormat(codecMime, sampleRate, channelCount);
    decodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    // 获取音频标识头
    decodecFormat.setByteBuffer("csd-0", csd0.second);
    // 配置解码器
    decodec.configure(decodecFormat, null, null, 0);
    // 启动解码器
    decodec.start();
  }

  // 创建AudioTrack
  private void setAudioTrack() {
    int sampleRate = 48000;
    int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 4;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      AudioTrack.Builder audioTrackBuild = new AudioTrack.Builder();
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
      audioTrackBuild.setBufferSizeInBytes(bufferSize);
      audioTrackBuild.setAudioAttributes(audioAttributesBulider.build());
      audioTrackBuild.setAudioFormat(audioFormat.build());
      // 4
      audioTrack = audioTrackBuild.build();
    } else audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    audioTrack.play();
  }

  // 创建音频放大器
  private void setLoudnessEnhancer() {
    loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
    loudnessEnhancer.setTargetGain(3500);
    loudnessEnhancer.setEnabled(true);
  }
}
