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

import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

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

// 此部分代码摘抄借鉴了著名投屏软件Scrcpy的开源代码(https://github.com/Genymobile/scrcpy/tree/master/server)
public final class Server {
  private static LocalSocket socket;
  private static FileDescriptor fileDescriptor;
  public static DataInputStream streamIn;

  public static final Object object = new Object();

  public static void main(String... args) {
    try {
      // 解析参数
      Options.parse(args);
      // 修改分辨率
      if (Options.setWidth != -1)
        new ProcessBuilder().command("bash", "-c", "wm size " + Options.setWidth + "x" + Options.setHeight).start();
      // 初始化
      setManagers();
      Device.init();
      // 连接
      connectClient();
      // 初始化子服务
      VideoEncode.init();
      boolean canAudio = AudioEncode.init();
      // 启动
      Thread videoOutThread = new Thread(Server::executeVideoOut);
      videoOutThread.setPriority(Thread.MAX_PRIORITY);
      Thread audioInThread = new Thread(Server::executeAudioIn);
      audioInThread.setPriority(Thread.MAX_PRIORITY);
      Thread audioOutThread = new Thread(Server::executeAudioOut);
      audioOutThread.setPriority(Thread.MAX_PRIORITY);
      Thread controlInThread = new Thread(Server::executeControlIn);
      controlInThread.setPriority(Thread.MAX_PRIORITY);
      videoOutThread.start();
      controlInThread.start();
      if (canAudio) {
        audioInThread.start();
        audioOutThread.start();
      }
      // 程序运行
      synchronized (object) {
        object.wait();
      }
      // 终止子服务
      videoOutThread.interrupt();
      controlInThread.interrupt();
      if (canAudio) {
        audioInThread.interrupt();
        audioOutThread.interrupt();
      }
      // 恢复分辨率
      if (Options.setWidth != -1)
        new ProcessBuilder().command("bash", "-c", "wm size reset").start();
    } catch (Exception ignored) {
    } finally {
      // 释放资源
      stop();
    }
  }

  // 关闭
  private static void stop() {
    for (int i = 0; i < 5; i++) {
      try {
        switch (i) {
          case 0:
            streamIn.close();
            socket.close();
            break;
          case 1:
            SurfaceControl.destroyDisplay(VideoEncode.display);
            break;
          case 2:
            Controller.checkScreenOff(false);
            break;
          case 3:
            VideoEncode.encedec.stop();
            VideoEncode.encedec.release();
            break;
          case 4:
            AudioEncode.audioCapture.stop();
            AudioEncode.audioCapture.release();
            AudioEncode.encedec.stop();
            AudioEncode.encedec.release();
            break;
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
    try (LocalServerSocket serverSocket = new LocalServerSocket("easycontrol")) {
      socket = serverSocket.accept();
      fileDescriptor = socket.getFileDescriptor();
      streamIn = new DataInputStream(socket.getInputStream());
    }
  }

  private static void executeVideoOut() {
    try {
      while (!Thread.interrupted()) {
        if (VideoEncode.isHasChangeRotation) {
          VideoEncode.isHasChangeRotation = false;
          VideoEncode.stopEncode();
          VideoEncode.initEncode();
        }
        VideoEncode.encodeOut();
      }
    } catch (Exception ignored) {
      synchronized (object) {
        object.notify();
      }
    }
  }

  private static int audioInLoopNum = 100;

  private static void executeAudioIn() {
    try {
      while (!Thread.interrupted()) {
        AudioEncode.encodeIn();
        audioInLoopNum++;
        if (audioInLoopNum > 100) {
          Controller.checkScreenOff(true);
          audioInLoopNum = 0;
        }
      }
    } catch (Exception ignored) {
      synchronized (object) {
        object.notify();
      }
    }
  }

  private static void executeAudioOut() {
    try {
      while (!Thread.interrupted()) AudioEncode.encodeOut();
    } catch (Exception ignored) {
      synchronized (object) {
        object.notify();
      }
    }
  }

  private static void executeControlIn() {
    try {
      while (!Thread.interrupted()) Controller.handleIn();
    } catch (Exception ignored) {
      synchronized (object) {
        object.notify();
      }
    }
  }

  public synchronized static void write(ByteBuffer byteBuffer) throws InterruptedIOException, ErrnoException {
    while (byteBuffer.remaining() > 0) Os.write(fileDescriptor, byteBuffer);
  }

}
