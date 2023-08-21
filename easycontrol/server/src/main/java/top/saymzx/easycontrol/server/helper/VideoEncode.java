package top.saymzx.easycontrol.server.helper;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Pair;
import android.view.Surface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Objects;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.Options;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;

public final class VideoEncode {
  public static MediaCodec encedec;
  private static MediaFormat encodecFormat;
  private static boolean isHasChangeRotation;

  public static IBinder display;

  public static Thread stream() {
    Thread thread = new StreamThread();
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  }

  static class StreamThread extends Thread implements Device.RotationListener {
    @Override
    public void run() {
      try {
        Device.rotationListener = this;
        // 创建Codec
        setVideoEncodec();
        // 创建显示器
        display = createDisplay();
        // 写入视频宽高
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putInt(Device.videoSize.first);
        byteBuffer.putInt(Device.videoSize.second);
        byteBuffer.flip();
        Server.writeFully(Server.videoStream, byteBuffer);
        // 旋转
        while (Server.isNormal) {
          // 重配置编码器宽高
          encodecFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
          encodecFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
          encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
          // 绑定Display和Surface
          setDisplaySurface(
              display,
              encedec.createInputSurface(),
              Device.layerStack,
              Device.deviceSize,
              Device.videoSize
          );
          // 启动编码
          encedec.start();
          encode();
          encedec.stop();
        }
      } catch (Exception ignored) {
        Server.isNormal = false;
      }
    }

    @Override
    public void onRotationChanged() {
      isHasChangeRotation = true;
    }
  }

  private static void setVideoEncodec() throws IOException {
    String codecMime = Objects.equals(Options.videoCodec, "h265") ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    encedec = MediaCodec.createEncoderByType(codecMime);
    encodecFormat = new MediaFormat();

    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime);
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, Options.videoBitRate);
    encodecFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
    encodecFormat.setInteger(
        MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    );
    encodecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
    encodecFormat.setLong(
        MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
        100_000 // 100ms
    );
    encodecFormat.setFloat("max-fps-to-encoder", Options.maxFps);
  }

  private static IBinder createDisplay() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S"
        .equals(Build.VERSION.CODENAME));
    return SurfaceControl.createDisplay("scrcpy_android", secure);
  }

  private static void setDisplaySurface(IBinder display, Surface surface, int layerStack, Pair<Integer, Integer> deviceSize, Pair<Integer, Integer> videoSize) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    SurfaceControl.openTransaction();
    try {
      SurfaceControl.setDisplaySurface(display, surface);
      SurfaceControl.setDisplayProjection(display, -1, new Rect(0, 0, deviceSize.first, deviceSize.second), new Rect(0, 0, videoSize.first, videoSize.second));
      SurfaceControl.setDisplayLayerStack(display, layerStack);
    } finally {
      SurfaceControl.closeTransaction();
    }
  }

  private static void encode() throws IOException {
    int outIndex;
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    while (!isHasChangeRotation && Server.isNormal) {
      // 找到已完成的输出缓冲区
      outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex < 0) continue;
      ByteBuffer byteBuffer = ByteBuffer.allocate(12);
      byteBuffer.putInt(bufferInfo.size);
      byteBuffer.putLong(System.currentTimeMillis());
      byteBuffer.flip();
      Server.writeFully(Server.videoStream, byteBuffer);
      Server.writeFully(Server.videoStream, encedec.getOutputBuffer(outIndex));
      encedec.releaseOutputBuffer(outIndex, false);
    }
    isHasChangeRotation = false;
  }

}
