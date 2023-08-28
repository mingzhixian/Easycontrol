/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server;

import android.annotation.SuppressLint;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.IBinder;
import android.os.IInterface;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Pair;

import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.Options;
import top.saymzx.easycontrol.server.helper.AudioEncode;
import top.saymzx.easycontrol.server.helper.Controller;
import top.saymzx.easycontrol.server.helper.VideoEncode;
import top.saymzx.easycontrol.server.wrappers.ClipboardManager;
import top.saymzx.easycontrol.server.wrappers.DisplayManager;
import top.saymzx.easycontrol.server.wrappers.InputManager;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;
import top.saymzx.easycontrol.server.wrappers.WindowManager;


public final class Server {
  private static LocalSocket videoStreamSocket;
  private static LocalSocket audioStreamSocket;
  private static LocalSocket controlStreamSocket;
  public static FileDescriptor videoStream;
  public static FileDescriptor audioStream;
  public static FileDescriptor controlStream;
  public static DataInputStream controlStreamIn;
  public static AtomicBoolean isNormal = new AtomicBoolean(true);

  public static void main(String... args) throws IOException, InterruptedException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // 初始化
    Options.parse(args);
    setManagers();
    Device.init();
    List<Thread> workThreads = new ArrayList<>();
    // 连接
    connectClient();
    // 启动
    if (isNormal.get()) {
      workThreads.add(VideoEncode.start());
      Pair<Thread, Thread> audioThreads = AudioEncode.start();
      if (audioThreads!=null){
        workThreads.add(audioThreads.first);
        workThreads.add(audioThreads.second);
      }
      workThreads.add(Controller.start());
      for (Thread thread : workThreads) thread.start();
      workThreads.get(0).join();
    }
    // 关闭
    stop();
  }

  // 关闭
  private static void stop() {
    System.out.print("停止");
    for (int i = 0; i < 8; i++) {
      try {
        switch (i) {
          case 1:
            videoStreamSocket.close();
          case 2:
            audioStreamSocket.close();
          case 3:
            controlStreamSocket.close();
          case 4:
            SurfaceControl.destroyDisplay(VideoEncode.display);
          case 5:
            VideoEncode.encedec.stop();
            VideoEncode.encedec.release();
          case 6:
            AudioEncode.audioCapture.stop();
            AudioEncode.audioCapture.release();
          case 7:
            AudioEncode.encedec.stop();
            AudioEncode.encedec.release();
        }
      } catch (Exception ignored) {
      }
    }
    Runtime.getRuntime().exit(0);
  }

  private static Method GET_SERVICE_METHOD;

  @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
  private static void setManagers() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
    // 1
    WindowManager.init(getService("window", "android.view.IWindowManager"));
    // 2
    DisplayManager.init(Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredMethod("getInstance").invoke(null));
    // 3
    Class<?> inputManagerClass;
    try {
      inputManagerClass = Class.forName("android.hardware.input.InputManagerGlobal");
    } catch (ClassNotFoundException e) {
      inputManagerClass = android.hardware.input.InputManager.class;
    }
    InputManager.init(inputManagerClass.getDeclaredMethod("getInstance").invoke(null));
    // 4
    ClipboardManager.init(getService("clipboard", "android.content.IClipboard"));
    // 5
    SurfaceControl.init();
  }

  private static IInterface getService(String service, String type) {
    try {
      IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
      Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
      return (IInterface) asInterfaceMethod.invoke(null, binder);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static void connectClient() throws IOException {
    LocalServerSocket serverSocket = new LocalServerSocket("easycontrol");
    videoStreamSocket = serverSocket.accept();
    audioStreamSocket = serverSocket.accept();
    controlStreamSocket = serverSocket.accept();
    videoStream = videoStreamSocket.getFileDescriptor();
    audioStream = audioStreamSocket.getFileDescriptor();
    controlStream = controlStreamSocket.getFileDescriptor();
    controlStreamIn = new DataInputStream(controlStreamSocket.getInputStream());
    serverSocket.close();
  }

  public static void writeFully(FileDescriptor fd, ByteBuffer from) throws IOException {
    int remaining = from.remaining();
    while (remaining > 0) {
      try {
        int len = Os.write(fd, from);
        if (len < 0) {
          throw new IOException("网络错误");
        }
        remaining -= len;
      } catch (ErrnoException e) {
        if (e.errno != OsConstants.EINTR) {
          throw new IOException(e);
        }
      }
    }
  }

  public static void writeFully(FileDescriptor fd, byte[] buffer) throws IOException {
    writeFully(fd, ByteBuffer.wrap(buffer, 0, buffer.length));
  }
}
