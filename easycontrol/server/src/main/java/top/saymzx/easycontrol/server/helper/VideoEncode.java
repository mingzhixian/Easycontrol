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
import android.system.ErrnoException;
import android.view.Surface;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.Options;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;

public final class VideoEncode {
  public static MediaCodec encedec;
  private static MediaFormat encodecFormat;
  public static boolean isHasChangeRotation = false;

  public static IBinder display;
  private static boolean tryCbr = true;

  public static void init() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ErrnoException {
    // 创建显示器
    display = SurfaceControl.createDisplay("easycontrol", Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(Build.VERSION.CODENAME)));
    // 写入视频宽高
    ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    byteBuffer.putInt(Device.videoSize.first);
    byteBuffer.putInt(Device.videoSize.second);
    byteBuffer.flip();
    Server.write(byteBuffer);
    // 创建Codec
    try {
      setVideoEncodec();
      initEncode();
    } catch (Exception ignored) {
      tryCbr = false;
      setVideoEncodec();
      initEncode();
    }
    encodeOut();
    encodeOut();
  }

  private static void setVideoEncodec() throws IOException {
    String codecMime = MediaFormat.MIMETYPE_VIDEO_AVC;
    encedec = MediaCodec.createEncoderByType(codecMime);
    encodecFormat = new MediaFormat();

    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime);
    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, Options.videoBitRate);
    encodecFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Options.maxFps);
    encodecFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      encodecFormat.setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1);
    }
    encodecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
    // 若在这个时间间隔内没有检测到足够大的场景变化，视频编码器将重复前一帧的内容，而不是编码全新的帧，以减少计算负担和节省带宽
    encodecFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 50_000);
    // CBR编码方式对网络传输比较好，其码率稳定，输出数据量稳定
    if (tryCbr)
      encodecFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
    encodecFormat.setFloat("max-fps-to-encoder", Options.maxFps);
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

  // 初始化编码器
  private static Surface surface;

  public static void initEncode() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    // 重配置编码器宽高
    encodecFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
    encodecFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    // 绑定Display和Surface
    surface = encedec.createInputSurface();
    setDisplaySurface(display, surface);
    // 启动编码
    encedec.start();
  }

  public static void stopEncode() {
    encedec.stop();
    encedec.reset();
    surface.release();
  }

  private static final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public static void encodeOut() throws InterruptedIOException, ErrnoException {
    try {
      // 找到已完成的输出缓冲区
      int outIndex;
      do outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1); while (outIndex < 0);
      ByteBuffer byteBuffer = ByteBuffer.allocate(5 + bufferInfo.size);
      byteBuffer.put((byte) 1);
      byteBuffer.putInt(bufferInfo.size);
      byteBuffer.put(encedec.getOutputBuffer(outIndex));
      byteBuffer.flip();
      Server.write(byteBuffer);
      encedec.releaseOutputBuffer(outIndex, false);
    } catch (IllegalStateException ignored) {
    }
  }

}
