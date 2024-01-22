package top.saymzx.easycontrol.app.client;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioDecode {
  private MediaCodec decodec;
  private AudioTrack audioTrack;
  private LoudnessEnhancer loudnessEnhancer;
  private final MediaCodec.Callback callback = new MediaCodec.Callback() {
    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inIndex) {
      checkDecode(intputDataQueue.poll(), inIndex);
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
      ByteBuffer buffer = decodec.getOutputBuffer(outIndex);
      if (buffer == null) return;
      audioTrack.write(buffer, bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING);
      decodec.releaseOutputBuffer(outIndex, false);
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat format) {
    }
  };

  public AudioDecode(boolean useOpus, ByteBuffer csd0) throws IOException {
    // 创建Codec
    setAudioDecodec(useOpus, csd0);
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

  public void decodeIn(ByteBuffer data) {
    checkDecode(data, intputBufferQueue.poll());
  }

  private final ConcurrentLinkedQueue<ByteBuffer> intputDataQueue = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Integer> intputBufferQueue = new ConcurrentLinkedQueue<>();

  private synchronized void checkDecode(ByteBuffer data, Integer inIndex) {
    if (data == null) {
      intputBufferQueue.offer(inIndex);
    } else if (inIndex == null) {
      intputDataQueue.offer(data);
    } else {
      decodec.getInputBuffer(inIndex).put(data);
      decodec.queueInputBuffer(inIndex, 0, data.capacity(), 0, 0);
    }
  }

  // 创建Codec
  private void setAudioDecodec(boolean useOpus, ByteBuffer csd0) throws IOException {
    // 创建解码器
    String codecMime = useOpus ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    // 音频参数
    int sampleRate = 48000;
    int channelCount = 2;
    int bitRate = 96000;
    MediaFormat decodecFormat = MediaFormat.createAudioFormat(codecMime, sampleRate, channelCount);
    decodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    // 获取音频标识头
    decodecFormat.setByteBuffer("csd-0", csd0);
    if (useOpus) {
      ByteBuffer csd12ByteBuffer = ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
      decodecFormat.setByteBuffer("csd-1", csd12ByteBuffer);
      decodecFormat.setByteBuffer("csd-2", csd12ByteBuffer);
    }
    // 异步解码
    decodec.setCallback(callback);
    // 配置解码器
    decodec.configure(decodecFormat, null, null, 0);
    // 启动解码器
    decodec.start();
  }

  // 创建AudioTrack
  private void setAudioTrack() {
    int sampleRate = 48000;
    int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      AudioTrack.Builder audioTrackBuild = new AudioTrack.Builder();
      // 1
      AudioAttributes.Builder audioAttributesBulider = new AudioAttributes.Builder();
      audioAttributesBulider.setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY);
      audioAttributesBulider.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
      // 2
      AudioFormat.Builder audioFormat = new AudioFormat.Builder();
      audioFormat.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
      audioFormat.setSampleRate(sampleRate);
      audioFormat.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
      // 3
      audioTrackBuild.setAudioAttributes(audioAttributesBulider.build())
        .setAudioFormat(audioFormat.build())
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(bufferSize);
      // 4
      audioTrack = audioTrackBuild.build();
    } else audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    audioTrack.play();
  }

  // 创建音频放大器
  private void setLoudnessEnhancer() {
    loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
    loudnessEnhancer.setTargetGain(2000);
    loudnessEnhancer.setEnabled(true);
  }
}
