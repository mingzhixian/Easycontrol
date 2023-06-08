#include <jni.h>
#include <string>
#include <oboe/Oboe.h>

// Oboe 音频流回调类
class OboeCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {

        // 需要生成 AudioFormat::Float 类型数据 , 该缓冲区类型也是该类型
        // 生产者需要检查该格式
        // oboe::AudioStream *audioStream 已经转换为适当的类型
        // 获取音频数据缓冲区
        auto *floatData = static_cast<float *>(audioData);

        return oboe::DataCallbackResult::Continue;
    }
};

// 创建 MyCallback 对象
OboeCallback oboeCallback = OboeCallback();
// 声明 Oboe 音频流
oboe::ManagedStream managedStream = oboe::ManagedStream();

// 缓冲区
char *audioData = new char[0];

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
    // 设置回调对象 , 注意要设置 AudioStreamCallback * 指针类型
    builder.setCallback(&oboeCallback);

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
Java_top_saymzx_scrcpy_android_Scrcpy_setAudioData(JNIEnv *env, jobject thiz, char *data) {
    audioData = data;
}