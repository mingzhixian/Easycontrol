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
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.Options;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;

public final class VideoEncode {
  private static MediaCodec encedec;
  private static MediaFormat encodecFormat;
  public static boolean isHasChangeConfig = false;
  private static boolean useH265;

  private static IBinder display;

  public static void init() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ErrnoException {
    useH265 = Options.useH265 && Device.isEncoderSupport("hevc");
    Server.write(new byte[]{(byte) (useH265 ? 1 : 0)});
    // 创建显示器
    display = SurfaceControl.createDisplay("easycontrol", Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(Build.VERSION.CODENAME)));
    // 创建Codec
    createEncodecFormat();
    startEncode();
  }

  private static void createEncodecFormat() throws IOException {
    String codecMime = useH265 ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    encedec = MediaCodec.createEncoderByType(codecMime);
    encodecFormat = new MediaFormat();

    encodecFormat.setString(MediaFormat.KEY_MIME, codecMime);

    encodecFormat.setInteger(MediaFormat.KEY_BIT_RATE, Options.maxVideoBit);
    encodecFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Options.maxFps);
    encodecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) encodecFormat.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, Options.maxFps * 3);
    encodecFormat.setFloat("max-fps-to-encoder", Options.maxFps);

    encodecFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 50_000);
    encodecFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
  }

  // 初始化编码器
  private static Surface surface;

  public static void startEncode() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    encodecFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
    encodecFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
    encedec.configure(encodecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    // 绑定Display和Surface
    surface = encedec.createInputSurface();
    setDisplaySurface(display, surface);
    // 启动编码
    encedec.start();
    ByteBuffer byteBuffer = ByteBuffer.allocate(9);
    byteBuffer.put((byte) 4);
    byteBuffer.putInt(Device.videoSize.first);
    byteBuffer.putInt(Device.videoSize.second);
    byteBuffer.flip();
    Server.write(byteBuffer.array());
  }

  public static void stopEncode() {
    encedec.stop();
    encedec.reset();
    surface.release();
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

  private static final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public static void encodeOut() throws IOException {
    try {
      // 找到已完成的输出缓冲区
      int outIndex;
      do outIndex = encedec.dequeueOutputBuffer(bufferInfo, -1); while (outIndex < 0);
      ByteBuffer byteBuffer = ByteBuffer.allocate(13 + bufferInfo.size);
      byteBuffer.put((byte) 1);
      byteBuffer.putInt(bufferInfo.size);
      byteBuffer.put(encedec.getOutputBuffer(outIndex));
      byteBuffer.putLong(bufferInfo.presentationTimeUs);
      byteBuffer.flip();
      Server.write(byteBuffer.array());
      encedec.releaseOutputBuffer(outIndex, false);
    } catch (IllegalStateException ignored) {
    }
  }

  public static void release() {
    try {
      stopEncode();
      encedec.release();
      SurfaceControl.destroyDisplay(display);
    } catch (Exception ignored) {
    }
  }

}
