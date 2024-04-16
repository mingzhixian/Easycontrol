/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

import android.content.IOnPrimaryClipChangedListener;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Pair;
import android.view.Display;
import android.view.IRotationWatcher;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.saymzx.easycontrol.server.helper.ControlPacket;
import top.saymzx.easycontrol.server.helper.VideoEncode;
import top.saymzx.easycontrol.server.wrappers.ClipboardManager;
import top.saymzx.easycontrol.server.wrappers.DisplayManager;
import top.saymzx.easycontrol.server.wrappers.InputManager;
import top.saymzx.easycontrol.server.wrappers.SurfaceControl;
import top.saymzx.easycontrol.server.wrappers.WindowManager;

public final class Device {
  private static int displayId = Display.DEFAULT_DISPLAY;
  private static VirtualDisplay virtualDisplay;
  public static Pair<Integer, Integer> realSize;
  public static DisplayInfo displayInfo;
  public static Pair<Integer, Integer> videoSize;
  private static boolean needReset = false;
  private static int oldScreenOffTimeout = 60000;

  public static void init() throws Exception {
    // 若启动单个应用则需创建虚拟Dispaly
    if (!Objects.equals(Options.startApp, "")) {
      virtualDisplay = DisplayManager.createVirtualDisplay();
      displayId = virtualDisplay.getDisplay().getDisplayId();
      startAndMoveAppToVirtualDisplay();
      needReset = true;
    }
    getRealSize();
    updateSize();
    // 旋转监听
    setRotationListener();
    // 剪切板监听
    if (Options.listenerClip) setClipBoardListener();
    // 设置不息屏
    if (Options.keepAwake) setKeepScreenLight();
  }

  // 打开并移动应用
  private static void startAndMoveAppToVirtualDisplay() throws IOException, InterruptedException {
    int appStackId = getAppStackId();
    if (appStackId == -1) {
      Device.execReadOutput("monkey -p " + Options.startApp + " -c android.intent.category.LAUNCHER 1");
      appStackId = getAppStackId();
    }
    if (appStackId == -1) throw new IOException("error app");
    Device.execReadOutput("am display move-stack " + appStackId + " " + displayId);
  }

  private static int getAppStackId() throws IOException, InterruptedException {
    String amStackList = Device.execReadOutput("am stack list");
    Matcher m = Pattern.compile("taskId=([0-9]+): " + Options.startApp).matcher(amStackList);
    if (!m.find()) return -1;
    return Integer.parseInt(Objects.requireNonNull(m.group(1)));
  }

  private static void getRealSize() throws IOException, InterruptedException {
    String output = Device.execReadOutput("wm size");
    String patStr;
    // 查看当前分辨率
    patStr = (output.contains("Override") ? "Override" : "Physical") + " size: (\\d+)x(\\d+)";
    Matcher matcher = Pattern.compile(patStr).matcher(output);
    if (matcher.find()) {
      String width = matcher.group(1);
      String height = matcher.group(2);
      if (width == null || height == null) return;
      realSize = new Pair<>(Integer.parseInt(width), Integer.parseInt(height));
    }
  }

  private static void updateSize() {
    displayInfo = DisplayManager.getDisplayInfo(displayId);
    boolean isPortrait = displayInfo.width < displayInfo.height;
    int major = isPortrait ? displayInfo.height : displayInfo.width;
    int minor = isPortrait ? displayInfo.width : displayInfo.height;
    if (major > Options.maxSize) {
      minor = minor * Options.maxSize / major;
      major = Options.maxSize;
    }
    // 某些厂商实现的解码器只接受16的倍数，所以需要缩放至最近参数
    minor = minor + 8 & ~15;
    major = major + 8 & ~15;
    videoSize = isPortrait ? new Pair<>(minor, major) : new Pair<>(major, minor);
  }

  // 修改分辨率
  public static void changeResolution(float targetRatio) {
    try {
      // 安全阈值(长宽比最多三倍)
      if (targetRatio > 3 || targetRatio < 0.34) return;
      // 没有获取到真实分辨率
      if (realSize == null) return;

      float originalRatio = (float) realSize.first / realSize.second;
      // 计算变化比率
      float ratioChange = targetRatio / originalRatio;
      // 根据比率变化确定新的长和宽
      int newWidth, newHeight;
      if (ratioChange > 1) {
        newWidth = realSize.first;
        newHeight = (int) (realSize.second / ratioChange);
      } else {
        newWidth = (int) (realSize.first * ratioChange);
        newHeight = realSize.second;
      }
      changeResolution(newWidth, newHeight);
    } catch (Exception ignored) {
    }
  }

  // 修改分辨率
  public static void changeResolution(int width, int height) {
    try {
      float originalRatio = (float) realSize.first / realSize.second;
      // 安全阈值(长宽比最多三倍)
      if (originalRatio > 3 || originalRatio < 0.34) return;

      needReset = true;

      // 缩放至16倍数
      width = width + 8 & ~15;
      height = height + 8 & ~15;
      // 避免分辨率相同，会触发安全机制导致系统崩溃
      if (width == height) width -= 16;

      // 修改分辨率
      if (virtualDisplay != null) virtualDisplay.resize(width, height, displayInfo.density);
      else Device.execReadOutput("wm size " + width + "x" + height);

      // 更新，需延迟一段时间
      Thread.sleep(200);
      updateSize();
      VideoEncode.isHasChangeConfig = true;
    } catch (Exception ignored) {
    }
  }

  // 恢复分辨率
  public static void fallbackResolution() throws IOException, InterruptedException {
    if (Device.needReset) {
      if (virtualDisplay != null) {
        int appStackId = getAppStackId();
        if (appStackId == -1) Device.execReadOutput("am display move-stack " + appStackId + " " + Display.DEFAULT_DISPLAY);
        virtualDisplay.release();
      } else {
        if (Device.realSize != null) Device.execReadOutput("wm size " + Device.realSize.first + "x" + Device.realSize.second);
        else Device.execReadOutput("wm size reset");
      }
    }
  }

  private static String nowClipboardText = "";

  public static void setClipBoardListener() {
    ClipboardManager.addPrimaryClipChangedListener(new IOnPrimaryClipChangedListener.Stub() {
      public void dispatchPrimaryClipChanged() {
        String newClipboardText = ClipboardManager.getText();
        if (newClipboardText == null) return;
        if (!newClipboardText.equals(nowClipboardText)) {
          nowClipboardText = newClipboardText;
          // 发送报文
          ControlPacket.sendClipboardEvent(nowClipboardText);
        }
      }
    });
  }

  public static void setClipboardText(String text) {
    nowClipboardText = text;
    ClipboardManager.setText(nowClipboardText);
  }

  private static void setRotationListener() {
    WindowManager.registerRotationWatcher(new IRotationWatcher.Stub() {
      public void onRotationChanged(int rotation) {
        updateSize();
        VideoEncode.isHasChangeConfig = true;
      }
    }, displayId);
  }

  private static final PointersState pointersState = new PointersState();

  public static void touchEvent(int action, Float x, Float y, int pointerId, int offsetTime) {
    Pointer pointer = pointersState.get(pointerId);

    if (pointer == null) {
      if (action != MotionEvent.ACTION_DOWN) return;
      pointer = pointersState.newPointer(pointerId, SystemClock.uptimeMillis() - 50);
    }

    pointer.x = x * displayInfo.width;
    pointer.y = y * displayInfo.height;
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

  public static void keyEvent(int keyCode, int meta) {
    long now = SystemClock.uptimeMillis();
    KeyEvent event1 = new KeyEvent(now, now, MotionEvent.ACTION_DOWN, keyCode, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
    KeyEvent event2 = new KeyEvent(now, now, MotionEvent.ACTION_UP, keyCode, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
    injectEvent(event1);
    injectEvent(event2);
  }

  private static void injectEvent(InputEvent inputEvent) {
    try {
      if (displayId != Display.DEFAULT_DISPLAY) InputManager.setDisplayId(inputEvent, displayId);
      InputManager.injectInputEvent(inputEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    } catch (Exception ignored) {
    }
  }

  public static void changeScreenPowerMode(int mode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      long[] physicalDisplayIds = SurfaceControl.getPhysicalDisplayIds();
      if (physicalDisplayIds == null) return;
      for (long physicalDisplayId : physicalDisplayIds) {
        IBinder token = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
        if (token != null) SurfaceControl.setDisplayPowerMode(token, mode);
      }
    } else {
      IBinder d = SurfaceControl.getBuiltInDisplay();
      if (d != null) SurfaceControl.setDisplayPowerMode(d, mode);
    }
  }

  public static void changePower(int mode) {
    if (mode == -1) keyEvent(26, 0);
    else {
      try {
        String output = execReadOutput("dumpsys deviceidle | grep mScreenOn");
        Boolean isScreenOn = null;
        if (output.contains("mScreenOn=true")) isScreenOn = true;
        else if (output.contains("mScreenOn=false")) isScreenOn = false;
        if (isScreenOn != null && isScreenOn ^ (mode == 1)) Device.keyEvent(26, 0);
      } catch (Exception ignored) {
      }
    }
  }

  public static void rotateDevice() {
    boolean accelerometerRotation = !WindowManager.isRotationFrozen(displayId);
    WindowManager.freezeRotation(displayId, (displayInfo.rotation == 0 || displayInfo.rotation == 3) ? 1 : 0);
    if (accelerometerRotation) WindowManager.thawRotation(displayId);
  }

  public static String execReadOutput(String cmd) throws IOException, InterruptedException {
    Process process = new ProcessBuilder().command("sh", "-c", cmd).start();
    StringBuilder builder = new StringBuilder();
    String line;
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      while ((line = bufferedReader.readLine()) != null) builder.append(line).append("\n");
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) throw new IOException("命令执行错误" + cmd);
    return builder.toString();
  }

  private static void setKeepScreenLight() {
    try {
      String output = execReadOutput("settings get system screen_off_timeout");
      // 使用正则表达式匹配数字
      Matcher matcher = Pattern.compile("\\d+").matcher(output);
      if (matcher.find()) {
        int timeout = Integer.parseInt(matcher.group());
        if (timeout >= 20 && timeout <= 60 * 30) oldScreenOffTimeout = timeout;
      }
      execReadOutput("settings put system screen_off_timeout 600000000");
    } catch (Exception ignored) {
    }
  }

  // 恢复自动锁定时间
  public static void fallbackScreenLightTimeout() throws IOException, InterruptedException {
    if (Options.keepAwake) Device.execReadOutput("settings put system screen_off_timeout " + Device.oldScreenOffTimeout);
  }

}
