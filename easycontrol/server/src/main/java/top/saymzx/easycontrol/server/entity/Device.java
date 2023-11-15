/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

import android.content.IOnPrimaryClipChangedListener;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Pair;
import android.view.IRotationWatcher;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.helper.VideoEncode;
import top.saymzx.easycontrol.server.wrappers.ClipboardManager;
import top.saymzx.easycontrol.server.wrappers.DisplayManager;
import top.saymzx.easycontrol.server.wrappers.InputManager;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;
import top.saymzx.easycontrol.server.wrappers.WindowManager;

public final class Device {
  private static Pair<Integer, Integer> realDeviceSize;
  public static Pair<Integer, Integer> deviceSize;
  public static boolean hasSetResolution = false;
  public static int deviceRotation;
  public static Pair<Integer, Integer> videoSize;

  private static String nowClipboardText = "";

  public static int displayId = 0;
  public static int layerStack;

  public static void init() throws IOException, InterruptedException {
    getRealDeviceSize();
    DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
    deviceSize = displayInfo.size;
    deviceRotation = displayInfo.rotation;
    layerStack = displayInfo.layerStack;
    computeVideoSize();
    // 旋转监听
    setRotationListener();
    // 剪切板监听
    setClipBoardListener();
  }

  // 获取真实的设备大小
  private static void getRealDeviceSize() {
    DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
    realDeviceSize = displayInfo.size;
    deviceRotation = displayInfo.rotation;
    if (deviceRotation == 1 || deviceRotation == 3) deviceSize = new Pair<>(deviceSize.second, deviceSize.first);
  }

  // 计算最佳的分辨率大小，按照主控端长宽比例缩放
  public static void changeDeviceSize(float reSize) throws IOException, InterruptedException {
    int newWidth;
    int newHeight;
    if (reSize > 1) {
      newWidth = realDeviceSize.first;
      newHeight = (int) (newWidth / reSize);
    } else {
      newHeight = realDeviceSize.second;
      newWidth = (int) (newHeight * reSize);
    }
    // 修改分辨率
    Device.execReadOutput("wm size " + newWidth + "x" + newHeight);
    deviceSize = new Pair<>(newWidth, newHeight);
    computeVideoSize();
    hasSetResolution = VideoEncode.isHasChangeConfig = true;
  }

  private static void computeVideoSize() {
    boolean isPortrait = deviceSize.first < deviceSize.second;
    int major = isPortrait ? deviceSize.second : deviceSize.first;
    int minor = isPortrait ? deviceSize.first : deviceSize.second;
    if (major > Options.maxSize) {
      minor = minor * Options.maxSize / major;
      major = Options.maxSize;
    }
    // h264只接受8的倍数，所以需要缩放至最近参数
    minor = minor + 4 & ~7;
    major = major + 4 & ~7;
    videoSize = isPortrait ? new Pair<>(minor, major) : new Pair<>(major, minor);
  }

  private static void setClipBoardListener() {
    ClipboardManager.addPrimaryClipChangedListener(new IOnPrimaryClipChangedListener.Stub() {
      public void dispatchPrimaryClipChanged() {
        String newClipboardText = ClipboardManager.getText();
        if (newClipboardText == null) return;
        if (!newClipboardText.equals(nowClipboardText)) {
          // 发送报文
          byte[] tmpTextByte = newClipboardText.getBytes(StandardCharsets.UTF_8);
          if (tmpTextByte.length > 5000) return;
          nowClipboardText = newClipboardText;
          ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
          byteBuffer.put((byte) 2);
          byteBuffer.putInt(tmpTextByte.length);
          byteBuffer.put(tmpTextByte);
          byteBuffer.flip();
          try {
            Server.write(byteBuffer);
          } catch (Exception ignored) {
            Server.errorClose();
          }
        }
      }
    });
  }

  private static void setRotationListener() {
    WindowManager.registerRotationWatcher(new IRotationWatcher.Stub() {
      public void onRotationChanged(int rotation) {
        if ((deviceRotation + rotation) % 2 != 0) {
          deviceSize = new Pair<>(deviceSize.second, deviceSize.first);
          videoSize = new Pair<>(videoSize.second, videoSize.first);
        }
        deviceRotation = rotation;
        VideoEncode.isHasChangeConfig = true;
      }
    }, displayId);
  }

  public static void setClipboardText(String text) {
    nowClipboardText = text;
    ClipboardManager.setText(nowClipboardText);
  }

  private static final PointersState pointersState = new PointersState();

  public static void touchEvent(int action, Float x, Float y, int pointerId, int offsetTime) {
    Pointer pointer = pointersState.get(pointerId);

    if (pointer == null) {
      if (action != MotionEvent.ACTION_DOWN) return;
      pointer = pointersState.newPointer(pointerId, SystemClock.uptimeMillis());
    }

    pointer.x = x * deviceSize.first;
    pointer.y = y * deviceSize.second;
    int pointerCount = pointersState.update();

    if (action == MotionEvent.ACTION_UP) {
      pointersState.remove(pointerId);
      if (pointerCount > 1) action = MotionEvent.ACTION_POINTER_UP | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    } else if (action == MotionEvent.ACTION_DOWN) {
      if (pointerCount > 1) action = MotionEvent.ACTION_POINTER_DOWN | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }
    MotionEvent event = MotionEvent.obtain(pointer.downTime, pointer.downTime + offsetTime, action, pointerCount, pointersState.pointerProperties, pointersState.pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    injectEvent(event);
  }

  public static void keyEvent(int keyCode) {
    long now = SystemClock.uptimeMillis();
    KeyEvent event1 = new KeyEvent(now, now, MotionEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
    KeyEvent event2 = new KeyEvent(now, now, MotionEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
    injectEvent(event1);
    injectEvent(event2);
  }

  private static void injectEvent(InputEvent inputEvent) {
    try {
      InputManager.injectInputEvent(inputEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    } catch (Exception ignored) {
    }
  }

  public static void setScreenPowerMode(int mode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      long[] physicalDisplayIds = SurfaceControl.getPhysicalDisplayIds();
      if (physicalDisplayIds == null) return;
      for (long physicalDisplayId : physicalDisplayIds) {
        IBinder token = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
        if (token != null) SurfaceControl.setDisplayPowerMode(token, mode);
      }
    } else {
      IBinder d = SurfaceControl.getBuiltInDisplay();
      if (d == null) return;
      SurfaceControl.setDisplayPowerMode(d, mode);
    }
  }

  public static String execReadOutput(String cmd) throws IOException, InterruptedException {
    Process process = new ProcessBuilder().command("sh", "-c", cmd).start();
    StringBuilder builder = new StringBuilder();
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) builder.append(scanner.nextLine()).append('\n');
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) throw new IOException("命令执行错误" + cmd);
    return builder.toString();
  }
}
