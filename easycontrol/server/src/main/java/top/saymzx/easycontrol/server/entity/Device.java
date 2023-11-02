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

import top.saymzx.easycontrol.server.Server;
import top.saymzx.easycontrol.server.helper.VideoEncode;
import top.saymzx.easycontrol.server.wrappers.ClipboardManager;
import top.saymzx.easycontrol.server.wrappers.DisplayManager;
import top.saymzx.easycontrol.server.wrappers.InputManager;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;
import top.saymzx.easycontrol.server.wrappers.WindowManager;

public final class Device {

  public static Pair<Integer, Integer> deviceSize;
  public static int deviceRotation;
  public static Pair<Integer, Integer> videoSize;

  private static String nowClipboardText = "";

  public static int displayId = 0;
  public static int layerStack;

  public static void init() throws IOException, InterruptedException {
    DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
    deviceSize = displayInfo.size;
    deviceRotation = displayInfo.rotation;
    // 修改分辨率
    if (Options.setWidth != -1) calculateSize();
    // 计算视频大小
    computeVideoSize();
    layerStack = displayInfo.layerStack;
    // 旋转监听
    setRotationListener();
    // 剪切板监听
    setClipBoardListener();
  }

  // 计算最佳的分辨率大小，按照主控端长宽比例缩放
  private static void calculateSize() throws IOException, InterruptedException {
    // 获取未旋转的原始分辨率
    if (deviceRotation == 1 || deviceRotation == 3)
      deviceSize = new Pair<>(deviceSize.second, deviceSize.first);
    // 保证方向一致
    if (deviceSize.first < deviceSize.second ^ Options.setWidth < Options.setHeight) {
      int tmp = Options.setWidth;
      Options.setWidth = Options.setHeight;
      Options.setHeight = tmp;
    }
    Pair<Integer, Integer> newScreenSize;
    int tmp1 = Options.setHeight * Device.deviceSize.first / Options.setWidth;
    // 横向最大不会超出
    if (Device.deviceSize.second > tmp1) newScreenSize = new Pair<>(Device.deviceSize.first, tmp1);
      // 竖向最大不会超出
    else
      newScreenSize = new Pair<>(Options.setWidth * Device.deviceSize.second / Options.setHeight, Device.deviceSize.second);
    // 修改分辨率
    Process process = new ProcessBuilder().command("sh", "-c", "wm size " + newScreenSize.first + "x" + newScreenSize.second).start();
    // 更新新的设备分辨率大小
    process.waitFor();
    DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
    deviceSize = displayInfo.size;
    deviceRotation = displayInfo.rotation;
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
          byteBuffer.put((byte) 3);
          byteBuffer.putInt(tmpTextByte.length);
          byteBuffer.put(tmpTextByte);
          byteBuffer.flip();
          try {
            Server.write(byteBuffer);
          } catch (Exception ignored) {
            synchronized (Server.object) {
              Server.object.notify();
            }
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
          byte[] bytes = new byte[]{4};
          try {
            Server.write(ByteBuffer.wrap(bytes));
          } catch (Exception ignored) {
            synchronized (Server.object) {
              Server.object.notify();
            }
          }
        }
        deviceRotation = rotation;
        VideoEncode.isHasChangeRotation = true;
      }
    }, displayId);
  }

  public static void setClipboardText(String text) {
    nowClipboardText = text;
    ClipboardManager.setText(nowClipboardText);
  }

  private static final PointersState pointersState = new PointersState();

  public static void touchEvent(int action, Float x, Float y, int pointerId) {
    Pointer pointer;
    long now = SystemClock.uptimeMillis();
    if (!pointersState.hasPointer(pointerId)) {
      if (action != MotionEvent.ACTION_DOWN) return;
      pointer = pointersState.newPointer(pointerId, now);
    } else pointer = pointersState.get(pointerId);

    pointer.x = x * deviceSize.first;
    pointer.y = y * deviceSize.second;

    int pointerCount = pointersState.update();

    if (action == MotionEvent.ACTION_UP) pointersState.remove(pointerId);

    if (pointerCount > 1) {
      if (action == MotionEvent.ACTION_UP) {
        action = MotionEvent.ACTION_POINTER_UP | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
      } else if (action == MotionEvent.ACTION_DOWN) {
        action = MotionEvent.ACTION_POINTER_DOWN | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
      }
    }

    MotionEvent event = MotionEvent.obtain(pointer.downTime, now, action, pointerCount, pointersState.pointerProperties, pointersState.pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
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
}
