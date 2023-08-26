/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.Options;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;

public final class VideoEncode {
  public static MediaCodec encedec;
  private static MediaFormat encodecFormat;
  private static final AtomicBoolean isHasChangeRotation = new AtomicBoolean(false);

  public static IBinder display;

  public static Thread start() {
    Thread thread = new StreamThread();
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  }

  static class StreamThread extends Thread implements Device.RotationListener {
    @Override
    public void run() {
      // 旋转
      Surface surface;
      for (int i = 0; i < 2; i++) {
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
          while (Server.isNormal.get()) {
            // 重配置编码器宽高
            encodecFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
            encodecFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
            encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 绑定Display和Surface
            surface = encedec.createInputSurface();
            setDisplaySurface(display, surface);
            // 启动编码
            encedec.start();
            encode();
            encedec.stop();
            encedec.reset();
            surface.release();
          }
        } catch (Exception ignored) {
          if (i == 0) tryCbr = false;
          else Server.isNormal.set(false);
        }
      }
    }

    @Override
    public void onRotationChanged() {
      isHasChangeRotation.set(true);
    }
  }

  private static boolean tryCbr = true;

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
    encodecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    encodecFormat.setLong(
        MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
        100_000 // 100ms
    );
    if (tryCbr)
      encodecFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
    encodecFormat.setFloat("max-fps-to-encoder", Options.maxFps);
  }

  private static IBinder createDisplay() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S"
        .equals(Build.VERSION.CODENAME));
    return SurfaceControl.createDisplay("easycontrol", secure);
  }

  private static void setDisplaySurface(IBinder display, Surface surface) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    SurfaceControl.openTransaction();
    try {
      SurfaceControl.setDisplaySurface(display, surface);
      SurfaceControl.setDisplayProjection(display, 0, new Rect(0, 0, Device.deviceSize.first, Device.deviceSize.second), new Rect(0, 0, Device.videoSize.first, Device.videoSize.second));
      SurfaceControl.setDisplayLayerStack(display, Device.layerStack);
    } finally {
      SurfaceControl.closeTransaction();
    }
  }

  private static void encode() throws IOException {
    int outIndex;
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    while (!isHasChangeRotation.getAndSet(false) && Server.isNormal.get()) {
      // 找到已完成的输出缓冲区
      outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex < 0) continue;
      ByteBuffer byteBuffer = ByteBuffer.allocate(9);
      byteBuffer.putInt(bufferInfo.size);
      byteBuffer.putInt((int) (System.currentTimeMillis() & 0x00000000FFFFFFFFL));
      if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) byteBuffer.put((byte) 1);
      else byteBuffer.put((byte) 0);
      byteBuffer.flip();
      Server.writeFully(Server.videoStream, byteBuffer);
      Server.writeFully(Server.videoStream, encedec.getOutputBuffer(outIndex));
      encedec.releaseOutputBuffer(outIndex, false);
    }
  }

}
