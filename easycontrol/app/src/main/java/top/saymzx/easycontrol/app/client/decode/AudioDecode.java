package top.saymzx.easycontrol.app.client.decode;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioDecode {
  private MediaCodec decodec;
  private AudioTrack audioTrack;
  private LoudnessEnhancer loudnessEnhancer;
  private static final int SAMPLE_RATE = 48000;
  private static final int CHANNELS = 2;
  private static final int BYTES_PER_SAMPLE = 2;
  private static final int AUDIO_PACKET_SIZE = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * 40 / 1000;
  private final MediaCodec.Callback callback = new MediaCodec.Callback() {
    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inIndex) {
      intputBufferQueue.offer(inIndex);
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
      try {
        ByteBuffer buffer = decodec.getOutputBuffer(outIndex);
        if (buffer == null) return;
        audioTrack.write(buffer, bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING);
        decodec.releaseOutputBuffer(outIndex, false);
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

  public AudioDecode(boolean useOpus, ByteBuffer csd0, Handler playHandler) throws IOException {
    // 创建Codec
    setAudioDecodec(useOpus, csd0, playHandler);
    // 创建AudioTrack
    setAudioTrack();
    // 创建音频放大器
    setLoudnessEnhancer();
  }

  public void release() {
    try {
      decodec.stop();
      decodec.release();
      audioTrack.stop();
      audioTrack.release();
      loudnessEnhancer.release();
    } catch (Exception ignored) {
    }
  }

  private final LinkedBlockingQueue<Integer> intputBufferQueue = new LinkedBlockingQueue<>();

  public void decodeIn(ByteBuffer data) throws InterruptedException {
    try {
      int inIndex = intputBufferQueue.take();
      decodec.getInputBuffer(inIndex).put(data);
      decodec.queueInputBuffer(inIndex, 0, data.capacity(), 0, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  private void setAudioDecodec(boolean useOpus, ByteBuffer csd0, Handler playHandler) throws IOException {
    // 创建解码器
    String codecMime = useOpus ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    // 音频参数
    int bitRate = 128000;
    MediaFormat decodecFormat = MediaFormat.createAudioFormat(codecMime, SAMPLE_RATE, CHANNELS);
    decodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    decodecFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_PACKET_SIZE);
    // 获取音频标识头
    decodecFormat.setByteBuffer("csd-0", csd0);
    if (useOpus) {
      ByteBuffer csd12ByteBuffer = ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
      decodecFormat.setByteBuffer("csd-1", csd12ByteBuffer);
      decodecFormat.setByteBuffer("csd-2", csd12ByteBuffer);
    }
    // 异步解码
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playHandler != null) {
      decodec.setCallback(callback, playHandler);
    } else decodec.setCallback(callback);
    // 配置解码器
    decodec.configure(decodecFormat, null, null, 0);
    // 启动解码器
    decodec.start();
  }

  // 创建AudioTrack
  private void setAudioTrack() {
    int bufferSize = Math.min(AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 8, 16 * AUDIO_PACKET_SIZE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      AudioTrack.Builder audioTrackBuild = new AudioTrack.Builder();
      // 1
      AudioAttributes.Builder audioAttributesBulider = new AudioAttributes.Builder();
      audioAttributesBulider.setUsage(AudioAttributes.USAGE_MEDIA);
      audioAttributesBulider.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
      // 2
      AudioFormat.Builder audioFormat = new AudioFormat.Builder();
      audioFormat.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
      audioFormat.setSampleRate(SAMPLE_RATE);
      audioFormat.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
      // 3
      audioTrackBuild.setAudioAttributes(audioAttributesBulider.build())
        .setAudioFormat(audioFormat.build())
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(bufferSize);
      // 4
      audioTrack = audioTrackBuild.build();
    } else audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    audioTrack.play();
  }

  // 创建音频放大器
  private void setLoudnessEnhancer() {
    loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
    loudnessEnhancer.setTargetGain(3000);
    loudnessEnhancer.setEnabled(true);
  }
}
