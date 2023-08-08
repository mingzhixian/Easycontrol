package top.saymzx.scrcpy.server.helper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.AttributionSource
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Looper
import android.os.Parcel
import top.saymzx.scrcpy.server.helper.FakeContext.Companion.get
import java.lang.ref.WeakReference
import java.lang.reflect.Method

object AudioCapture {
  private lateinit var recorder: AudioRecord

  fun start(): AudioRecord {
    recorder = try {
      createAudioRecord()
    } catch (e: NullPointerException) {
      createAudioRecordVivo()
    }
    recorder.startRecording()
    return recorder
  }

  const val SAMPLE_RATE = 44100
  private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
  const val CHANNELS = 2
  private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_LEFT or AudioFormat.CHANNEL_IN_RIGHT
  private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
  private const val BYTES_PER_SAMPLE = 2

  fun millisToBytes(millis: Int): Int {
    return SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * millis / 1000
  }

  @SuppressLint("WrongConstant", "MissingPermission")
  private fun createAudioRecord(): AudioRecord {
    val audioRecordBuilder = AudioRecord.Builder()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // On older APIs, Workarounds.fillAppInfo() must be called beforehand
      audioRecordBuilder.setContext(get())
    }
    audioRecordBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
    val audioFormatBuilder = AudioFormat.Builder()
    audioFormatBuilder.setEncoding(ENCODING)
    audioFormatBuilder.setSampleRate(SAMPLE_RATE)
    audioFormatBuilder.setChannelMask(CHANNEL_CONFIG)
    audioRecordBuilder.setAudioFormat(audioFormatBuilder.build())
    audioRecordBuilder.setBufferSizeInBytes(
      AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        ENCODING
      )
    )
    return audioRecordBuilder.build()
  }

  @TargetApi(Build.VERSION_CODES.R)
  @SuppressLint("WrongConstant,MissingPermission,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
  private fun createAudioRecordVivo(): AudioRecord {
    // Vivo (and maybe some other third-party ROMs) modified `AudioRecord`'s constructor, requiring `Context`s from real App environment.
    //
    // This method invokes the `AudioRecord(long nativeRecordInJavaObj)` constructor to create an empty `AudioRecord` instance, then uses
    // reflections to initialize it like the normal constructor do (or the `AudioRecord.Builder.build()` method do).
    // As a result, the modified code was not executed.
    return try {
      val audioRecordConstructor =
        AudioRecord::class.java.getDeclaredConstructor(Long::class.javaPrimitiveType)
      audioRecordConstructor.isAccessible = true
      val audioRecord = audioRecordConstructor.newInstance(0L)

      // audioRecord.mRecordingState = RECORDSTATE_STOPPED;
      val mRecordingStateField = AudioRecord::class.java.getDeclaredField("mRecordingState")
      mRecordingStateField.isAccessible = true
      mRecordingStateField[audioRecord] = AudioRecord.RECORDSTATE_STOPPED
      var looper = Looper.myLooper()
      if (looper == null) {
        looper = Looper.getMainLooper()
      }

      // audioRecord.mInitializationLooper = looper;
      val mInitializationLooperField =
        AudioRecord::class.java.getDeclaredField("mInitializationLooper")
      mInitializationLooperField.isAccessible = true
      mInitializationLooperField[audioRecord] = looper

      // Create `AudioAttributes` with fixed capture preset
      val audioAttributesBuilder = AudioAttributes.Builder()
      val setInternalCapturePresetMethod =
        AudioAttributes.Builder::class.java.getMethod(
          "setInternalCapturePreset",
          Int::class.javaPrimitiveType
        )
      setInternalCapturePresetMethod.invoke(
        audioAttributesBuilder,
        MediaRecorder.AudioSource.REMOTE_SUBMIX
      )
      val attributes = audioAttributesBuilder.build()

      // audioRecord.mAudioAttributes = attributes;
      val mAudioAttributesField = AudioRecord::class.java.getDeclaredField("mAudioAttributes")
      mAudioAttributesField.isAccessible = true
      mAudioAttributesField[audioRecord] = attributes

      // audioRecord.audioParamCheck(capturePreset, sampleRate, encoding);
      val audioParamCheckMethod = AudioRecord::class.java.getDeclaredMethod(
        "audioParamCheck",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
      )
      audioParamCheckMethod.isAccessible = true
      audioParamCheckMethod.invoke(
        audioRecord,
        MediaRecorder.AudioSource.REMOTE_SUBMIX,
        SAMPLE_RATE,
        ENCODING
      )

      // audioRecord.mChannelCount = channels
      val mChannelCountField = AudioRecord::class.java.getDeclaredField("mChannelCount")
      mChannelCountField.isAccessible = true
      mChannelCountField[audioRecord] = CHANNELS

      // audioRecord.mChannelMask = channelMask
      val mChannelMaskField = AudioRecord::class.java.getDeclaredField("mChannelMask")
      mChannelMaskField.isAccessible = true
      mChannelMaskField[audioRecord] = CHANNEL_MASK
      val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
      val bufferSizeInBytes = minBufferSize * 8

      // audioRecord.audioBuffSizeCheck(bufferSizeInBytes)
      val audioBuffSizeCheckMethod =
        AudioRecord::class.java.getDeclaredMethod(
          "audioBuffSizeCheck",
          Int::class.javaPrimitiveType
        )
      audioBuffSizeCheckMethod.isAccessible = true
      audioBuffSizeCheckMethod.invoke(audioRecord, bufferSizeInBytes)
      val channelIndexMask = 0
      val sampleRateArray = intArrayOf(SAMPLE_RATE)
      val session = intArrayOf(AudioManager.AUDIO_SESSION_ID_GENERATE)
      var initResult: Int
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        // private native final int native_setup(Object audiorecord_this,
        // Object /*AudioAttributes*/ attributes,
        // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
        // int buffSizeInBytes, int[] sessionId, String opPackageName,
        // long nativeRecordInJavaObj);
        val nativeSetupMethod = AudioRecord::class.java.getDeclaredMethod(
          "native_setup",
          Any::class.java,
          Any::class.java,
          IntArray::class.java,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          IntArray::class.java,
          String::class.java,
          Long::class.javaPrimitiveType
        )
        nativeSetupMethod.isAccessible = true
        initResult = nativeSetupMethod.invoke(
          audioRecord,
          WeakReference(audioRecord),
          attributes,
          sampleRateArray,
          CHANNEL_MASK,
          channelIndexMask,
          audioRecord.audioFormat,
          bufferSizeInBytes,
          session,
          get().opPackageName,
          0L
        ) as Int
      } else {
        // Assume `context` is never `null`
        val attributionSource: AttributionSource = get().attributionSource

        // Assume `attributionSource.getPackageName()` is never null

        // ScopedParcelState attributionSourceState = attributionSource.asScopedParcelState()
        val asScopedParcelStateMethod =
          AttributionSource::class.java.getDeclaredMethod("asScopedParcelState")
        asScopedParcelStateMethod.isAccessible = true
        (asScopedParcelStateMethod.invoke(attributionSource) as AutoCloseable).use { attributionSourceState ->
          val getParcelMethod: Method =
            attributionSourceState.javaClass.getDeclaredMethod("getParcel")
          val attributionSourceParcel = getParcelMethod.invoke(attributionSourceState) as Parcel

          // private native int native_setup(Object audiorecordThis,
          // Object /*AudioAttributes*/ attributes,
          // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
          // int buffSizeInBytes, int[] sessionId, @NonNull Parcel attributionSource,
          // long nativeRecordInJavaObj, int maxSharedAudioHistoryMs);
          val nativeSetupMethod = AudioRecord::class.java.getDeclaredMethod(
            "native_setup",
            Any::class.java,
            Any::class.java,
            IntArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            IntArray::class.java,
            Parcel::class.java,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
          )
          nativeSetupMethod.isAccessible = true
          initResult = nativeSetupMethod.invoke(
            audioRecord,
            WeakReference(audioRecord),
            attributes,
            sampleRateArray,
            CHANNEL_MASK,
            channelIndexMask,
            audioRecord.audioFormat,
            bufferSizeInBytes,
            session,
            attributionSourceParcel,
            0L,
            0
          ) as Int
        }
      }
      if (initResult != AudioRecord.SUCCESS) {
        throw RuntimeException("Cannot create AudioRecord")
      }

      // mSampleRate = sampleRate[0]
      val mSampleRateField = AudioRecord::class.java.getDeclaredField("mSampleRate")
      mSampleRateField.isAccessible = true
      mSampleRateField[audioRecord] = sampleRateArray[0]

      // audioRecord.mSessionId = session[0]
      val mSessionIdField = AudioRecord::class.java.getDeclaredField("mSessionId")
      mSessionIdField.isAccessible = true
      mSessionIdField[audioRecord] = session[0]

      // audioRecord.mState = AudioRecord.STATE_INITIALIZED
      val mStateField = AudioRecord::class.java.getDeclaredField("mState")
      mStateField.isAccessible = true
      mStateField[audioRecord] = AudioRecord.STATE_INITIALIZED
      audioRecord
    } catch (e: Exception) {
      throw RuntimeException("Cannot create AudioRecord")
    }
  }
}