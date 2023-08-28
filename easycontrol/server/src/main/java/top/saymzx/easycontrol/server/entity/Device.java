/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

import android.annotation.SuppressLint;
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
  public static RotationListener rotationListener;

  public interface RotationListener {
    void onRotationChanged();
  }

  public static int displayId = 0;
  public static int layerStack;

  public static void init() {
    DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
    deviceSize = displayInfo.size;
    deviceRotation = displayInfo.rotation;
    // 计算视频大小
    computeVideoSize();
    layerStack = displayInfo.layerStack;
    // 旋转监听
    setRotationListener();
    // 剪切板监听
    setClipBoardListener();
    // 初始化指针
    initPointers();
  }

  // 初始化指针
  private static void initPointers() {
    for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
      MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
      props.toolType = MotionEvent.TOOL_TYPE_FINGER;

      MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
      coords.orientation = 0;
      coords.size = 0;
      coords.pressure = 0;

      pointerProperties[i] = props;
      pointerCoords[i] = coords;
    }
  }

  private static void computeVideoSize() {
    // h264只接受8的倍数，所以需要缩放至最近参数
    boolean isPortrait = deviceSize.first < deviceSize.second;
    int major = isPortrait ? deviceSize.second : deviceSize.first;
    int minor = isPortrait ? deviceSize.first : deviceSize.second;
    if (major > Options.maxSize) {
      int minorExact = minor * Options.maxSize / major;
      minor = minorExact + 4 & ~7;
      major = Options.maxSize;
    }
    videoSize = isPortrait ? new Pair<>(minor, major) : new Pair<>(major, minor);
  }

  private static void setRotationListener() {
    WindowManager.registerRotationWatcher(new IRotationWatcher.Stub() {
      public void onRotationChanged(int rotation) {
        if ((deviceRotation + rotation) % 2 != 0) {
          deviceSize = new Pair<>(deviceSize.second, deviceSize.first);
          videoSize = new Pair<>(videoSize.second, videoSize.first);
          try {
            byte[] bytes = new byte[]{21};
            synchronized (Server.controlStream) {
              Server.writeFully(Server.controlStream, bytes);
            }
          } catch (Exception ignored) {
            Server.isNormal.set(false);
            return;
          }
        }
        deviceRotation = rotation;
        rotationListener.onRotationChanged();
      }
    }, displayId);
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
          try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(5);
            byteBuffer.put((byte) 20);
            byteBuffer.putInt(tmpTextByte.length);
            byteBuffer.flip();
            synchronized (Server.controlStream) {
              Server.writeFully(Server.controlStream, byteBuffer);
              Server.writeFully(Server.controlStream, tmpTextByte);
            }
          } catch (IOException ignored) {
            Server.isNormal.set(false);
          }
        }
      }
    });
  }

  public static void setClipboardText(String text) {
    nowClipboardText = text;
    ClipboardManager.setText(nowClipboardText);
  }

  private static long lastTouchDown;
  private static final PointersState pointersState = new PointersState();
  private static final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
  private static final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];

  @SuppressLint({"Recycle"})
  public static void touchEvent(int action, Pair<Float, Float> position, int pointerId, int vertical) {
    // 旋转中的触摸事件
    if (vertical > 0 ^ videoSize.first > videoSize.second) if (action == MotionEvent.ACTION_UP)
      pointersState.get(pointersState.getPointerIndex(pointerId)).isUp = true;
    else return;
    // 正常触摸
    pointerProperties[pointerId].id = pointerId;
    long now = SystemClock.uptimeMillis();
    int pointerIndex = pointersState.getPointerIndex(pointerId);
    if (pointerIndex == -1) {
      return;
    }
    Pointer pointer = pointersState.get(pointerIndex);
    pointer.x = position.first * deviceSize.first;
    pointer.y = position.second * deviceSize.second;

    pointer.isUp = action == MotionEvent.ACTION_UP;

    int pointerCount = pointersState.update(pointerProperties, pointerCoords);
    int newAction = action;
    if (pointerCount == 1) {
      if (action == MotionEvent.ACTION_DOWN) {
        lastTouchDown = now;
      }
    } else {
      if (action == MotionEvent.ACTION_UP) {
        newAction = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
      } else if (action == MotionEvent.ACTION_DOWN) {
        newAction = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
      }
    }
    MotionEvent event = MotionEvent.obtain(lastTouchDown, now, newAction, pointerCount, pointerProperties, pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
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
