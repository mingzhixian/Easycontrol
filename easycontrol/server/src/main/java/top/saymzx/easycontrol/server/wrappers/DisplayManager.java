/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.content.Context;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.os.Build;
import android.view.Display;
import android.view.Surface;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.saymzx.easycontrol.server.entity.Device;
import top.saymzx.easycontrol.server.entity.DisplayInfo;
import top.saymzx.easycontrol.server.helper.FakeContext;

public final class DisplayManager {
  private static Object manager;

  private static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
  private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;
  private static final int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
  private static final int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
  private static final int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;

  public static void init(Object m) {
    manager = m;
  }

  private static DisplayInfo getDisplayInfoFromDumpsysDisplay(int displayId) {
    try {
      String dumpsysDisplayOutput = Device.execReadOutput("dumpsys display");
      Matcher m = Pattern.compile("mOverrideDisplayInfo=DisplayInfo.*?, displayId " + displayId + ".*?, real ([0-9]+) x ([0-9]+).*?, rotation ([0-9]+).*?, density ([0-9]+).*?, layerStack ([0-9]+)").matcher(dumpsysDisplayOutput);
      if (!m.find()) return null;
      int width = Integer.parseInt(Objects.requireNonNull(m.group(1)));
      int height = Integer.parseInt(Objects.requireNonNull(m.group(2)));
      int rotation = Integer.parseInt(Objects.requireNonNull(m.group(3)));
      int density = Integer.parseInt(Objects.requireNonNull(m.group(4)));
      int layerStack = Integer.parseInt(Objects.requireNonNull(m.group(5)));
      return new DisplayInfo(displayId, width, height, rotation, density, layerStack);
    } catch (Exception e) {
      return null;
    }
  }

  public static DisplayInfo getDisplayInfo(int displayId) {
    try {
      Object displayInfo = manager.getClass().getMethod("getDisplayInfo", int.class).invoke(manager, displayId);
      // fallback when displayInfo is null
      if (displayInfo == null) return getDisplayInfoFromDumpsysDisplay(displayId);
      Class<?> cls = displayInfo.getClass();
      // width and height already take the rotation into account
      int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
      int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
      int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
      int layerStack = cls.getDeclaredField("layerStack").getInt(displayInfo);
      int density = cls.getDeclaredField("logicalDensityDpi").getInt(displayInfo);
      return new DisplayInfo(displayId, width, height, rotation, density, layerStack);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  // 此处大量借鉴了 群友 @○_○ 所编写的易控车机版本相应功能
  public static VirtualDisplay createVirtualDisplay() throws Exception {
    DisplayInfo realDisplayinfo = getDisplayInfo(Display.DEFAULT_DISPLAY);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      throw new Exception("Virtual display is not supported before Android 11");
    }

    int flags = android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL | android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED;

    Surface surface = MediaCodec.createPersistentInputSurface();
    android.hardware.display.DisplayManager displayManager = android.hardware.display.DisplayManager.class.getDeclaredConstructor(Context.class).newInstance(FakeContext.get());
    return displayManager.createVirtualDisplay("easycontrol", realDisplayinfo.width, realDisplayinfo.height, realDisplayinfo.density, surface, flags);
  }
}
