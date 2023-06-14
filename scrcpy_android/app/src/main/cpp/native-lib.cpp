#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include <android/log.h>

// 声明 Oboe 音频流
oboe::ManagedStream managedStream = oboe::ManagedStream();

extern "C"
JNIEXPORT void JNICALL
Java_top_saymzx_scrcpy_android_Scrcpy_setOboe(JNIEnv *env, jobject thiz) {
    // 1. 音频流构建器
    oboe::AudioStreamBuilder builder = oboe::AudioStreamBuilder();
    // 设置音频流方向
    builder.setDirection(oboe::Direction::Output);
    // 设置性能优先级
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    // 设置共享模式
    builder.setSharingMode(oboe::SharingMode::Shared);
    // 设置音频采样格式
    builder.setFormat(oboe::AudioFormat::Float);
    // 设置声道数 , 单声道/立体声
    builder.setChannelCount(oboe::ChannelCount::Stereo);
    // 设置采样率
    builder.setSampleRate(48000);

    // 2. 通过 AudioStreamBuilder 打开 Oboe 音频流
    builder.openManagedStream(managedStream);

    // 3. 开始播放
    managedStream->requestStart();
}

extern "C"
JNIEXPORT void JNICALL
Java_top_saymzx_scrcpy_android_Scrcpy_stopOboe(JNIEnv *env, jobject thiz) {
    managedStream->requestStop();
}

extern "C"
JNIEXPORT void JNICALL
Java_top_saymzx_scrcpy_android_Scrcpy_setByte(JNIEnv *env, jobject thiz,
                                              jbyteArray byteArray, int size) {
    auto byteArrayC = env->GetByteArrayElements(byteArray, JNI_FALSE);
    managedStream->write((float *) byteArrayC, size >> 4, 0);
    env->ReleaseByteArrayElements(byteArray, byteArrayC, 0);
}