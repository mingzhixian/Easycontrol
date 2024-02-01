/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Looper;
import android.os.Parcel;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AudioCapture {
  public static final int SAMPLE_RATE = 48000;
  private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
  public static final int CHANNELS = 2;
  private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_LEFT | AudioFormat.CHANNEL_IN_RIGHT;
  public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
  private static final int BYTES_PER_SAMPLE = 2;
  public static final int AUDIO_PACKET_SIZE = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * 40 / 1000;
  private static final int MINI_BUFFER_SIZE = Math.min(AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING), AUDIO_PACKET_SIZE * 4);

  public static AudioRecord init() {
    AudioRecord recorder;
    try {
      recorder = createAudioRecord();
    } catch (NullPointerException ignored) {
      recorder = createAudioRecordVivo();
    }
    recorder.startRecording();
    return recorder;
  }

  @SuppressLint({"WrongConstant", "MissingPermission"})
  private static AudioRecord createAudioRecord() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      AudioRecord.Builder audioRecordBuilder = new AudioRecord.Builder();
      if (VERSION.SDK_INT >= 31) audioRecordBuilder.setContext(FakeContext.get());

      audioRecordBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
      AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
      audioFormatBuilder.setEncoding(ENCODING);
      audioFormatBuilder.setSampleRate(SAMPLE_RATE);
      audioFormatBuilder.setChannelMask(CHANNEL_CONFIG);
      audioRecordBuilder.setAudioFormat(audioFormatBuilder.build());
      audioRecordBuilder.setBufferSizeInBytes(MINI_BUFFER_SIZE);
      return audioRecordBuilder.build();
    }
    return null;
  }

  @TargetApi(Build.VERSION_CODES.R)
  @SuppressLint("WrongConstant,MissingPermission,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
  private static AudioRecord createAudioRecordVivo() {
    try {
      // AudioRecord audioRecord = new AudioRecord(0L);
      Constructor<AudioRecord> audioRecordConstructor = AudioRecord.class.getDeclaredConstructor(long.class);
      audioRecordConstructor.setAccessible(true);
      AudioRecord audioRecord = audioRecordConstructor.newInstance(0L);

      // audioRecord.mRecordingState = RECORDSTATE_STOPPED;
      Field mRecordingStateField = AudioRecord.class.getDeclaredField("mRecordingState");
      mRecordingStateField.setAccessible(true);
      mRecordingStateField.set(audioRecord, AudioRecord.RECORDSTATE_STOPPED);

      Looper looper = Looper.myLooper();
      if (looper == null) {
        looper = Looper.getMainLooper();
      }

      // audioRecord.mInitializationLooper = looper;
      Field mInitializationLooperField = AudioRecord.class.getDeclaredField("mInitializationLooper");
      mInitializationLooperField.setAccessible(true);
      mInitializationLooperField.set(audioRecord, looper);

      // Create `AudioAttributes` with fixed capture preset
      AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
      Method setInternalCapturePresetMethod = AudioAttributes.Builder.class.getMethod("setInternalCapturePreset", int.class);
      setInternalCapturePresetMethod.invoke(audioAttributesBuilder, MediaRecorder.AudioSource.REMOTE_SUBMIX);
      AudioAttributes attributes = audioAttributesBuilder.build();

      // audioRecord.mAudioAttributes = attributes;
      Field mAudioAttributesField = AudioRecord.class.getDeclaredField("mAudioAttributes");
      mAudioAttributesField.setAccessible(true);
      mAudioAttributesField.set(audioRecord, attributes);

      // audioRecord.audioParamCheck(capturePreset, sampleRate, encoding);
      Method audioParamCheckMethod = AudioRecord.class.getDeclaredMethod("audioParamCheck", int.class, int.class, int.class);
      audioParamCheckMethod.setAccessible(true);
      audioParamCheckMethod.invoke(audioRecord, MediaRecorder.AudioSource.REMOTE_SUBMIX,
        SAMPLE_RATE,
        ENCODING);

      // audioRecord.mChannelCount = channels
      Field mChannelCountField = AudioRecord.class.getDeclaredField("mChannelCount");
      mChannelCountField.setAccessible(true);
      mChannelCountField.set(audioRecord, CHANNELS);

      // audioRecord.mChannelMask = channelMask
      Field mChannelMaskField = AudioRecord.class.getDeclaredField("mChannelMask");
      mChannelMaskField.setAccessible(true);
      mChannelMaskField.set(audioRecord, CHANNEL_MASK);

      // audioRecord.audioBuffSizeCheck(bufferSizeInBytes)
      Method audioBuffSizeCheckMethod = AudioRecord.class.getDeclaredMethod("audioBuffSizeCheck", int.class);
      audioBuffSizeCheckMethod.setAccessible(true);
      audioBuffSizeCheckMethod.invoke(audioRecord, MINI_BUFFER_SIZE);

      final int channelIndexMask = 0;

      int[] sampleRateArray = new int[]{SAMPLE_RATE};
      int[] session = new int[]{AudioManager.AUDIO_SESSION_ID_GENERATE};

      int initResult;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        // private native final int native_setup(Object audiorecord_this,
        // Object /*AudioAttributes*/ attributes,
        // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
        // int buffSizeInBytes, int[] sessionId, String opPackageName,
        // long nativeRecordInJavaObj);
        Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class, int.class,
          int.class, int.class, int.class, int[].class, String.class, long.class);
        nativeSetupMethod.setAccessible(true);
        initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes, sampleRateArray,
          CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), MINI_BUFFER_SIZE, session, FakeContext.get().getOpPackageName(),
          0L);
      } else {
        // Assume `context` is never `null`
        AttributionSource attributionSource = FakeContext.get().getAttributionSource();

        // Assume `attributionSource.getPackageName()` is never null

        // ScopedParcelState attributionSourceState = attributionSource.asScopedParcelState()
        Method asScopedParcelStateMethod = AttributionSource.class.getDeclaredMethod("asScopedParcelState");
        asScopedParcelStateMethod.setAccessible(true);

        try (AutoCloseable attributionSourceState = (AutoCloseable) asScopedParcelStateMethod.invoke(attributionSource)) {
          Method getParcelMethod = attributionSourceState.getClass().getDeclaredMethod("getParcel");
          Parcel attributionSourceParcel = (Parcel) getParcelMethod.invoke(attributionSourceState);

          // private native int native_setup(Object audiorecordThis,
          // Object /*AudioAttributes*/ attributes,
          // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
          // int buffSizeInBytes, int[] sessionId, @NonNull Parcel attributionSource,
          // long nativeRecordInJavaObj, int maxSharedAudioHistoryMs);
          Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class, int.class,
            int.class, int.class, int.class, int[].class, Parcel.class, long.class, int.class);
          nativeSetupMethod.setAccessible(true);
          initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes, sampleRateArray,
            CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), MINI_BUFFER_SIZE, session, attributionSourceParcel, 0L, 0);
        }
      }

      if (initResult != AudioRecord.SUCCESS) {
        throw new RuntimeException("Cannot create AudioRecord");
      }

      // mSampleRate = sampleRate[0]
      Field mSampleRateField = AudioRecord.class.getDeclaredField("mSampleRate");
      mSampleRateField.setAccessible(true);
      mSampleRateField.set(audioRecord, sampleRateArray[0]);

      // audioRecord.mSessionId = session[0]
      Field mSessionIdField = AudioRecord.class.getDeclaredField("mSessionId");
      mSessionIdField.setAccessible(true);
      mSessionIdField.set(audioRecord, session[0]);

      // audioRecord.mState = AudioRecord.STATE_INITIALIZED
      Field mStateField = AudioRecord.class.getDeclaredField("mState");
      mStateField.setAccessible(true);
      mStateField.set(audioRecord, AudioRecord.STATE_INITIALIZED);

      return audioRecord;
    } catch (Exception e) {
      throw new RuntimeException("Cannot create AudioRecord");
    }
  }
}
