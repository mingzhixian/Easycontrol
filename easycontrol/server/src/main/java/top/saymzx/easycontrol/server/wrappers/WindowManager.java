/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.Display;
import android.view.IRotationWatcher;

import java.lang.reflect.Method;

public final class WindowManager {
  private static IInterface manager;
  private static Class<?> CLASS;
  private static Method freezeRotationMethod = null;
  private static Method freezeDisplayRotationMethod = null;
  private static Method isRotationFrozenMethod = null;
  private static Method isDisplayRotationFrozenMethod = null;
  private static Method thawRotationMethod = null;
  private static Method thawDisplayRotationMethod = null;

  public static void init(IInterface m) {
    manager = m;
    CLASS = manager.getClass();
    try {
      freezeRotationMethod = manager.getClass().getMethod("freezeRotation", int.class);
      freezeDisplayRotationMethod = manager.getClass().getMethod("freezeDisplayRotation", int.class, int.class);
    } catch (Exception ignored) {
    }
    try {
      isRotationFrozenMethod = manager.getClass().getMethod("isRotationFrozen");
      isDisplayRotationFrozenMethod = manager.getClass().getMethod("isDisplayRotationFrozen", int.class);
    } catch (Exception ignored) {
    }
    try {
      thawRotationMethod = manager.getClass().getMethod("thawRotation");
      thawDisplayRotationMethod = manager.getClass().getMethod("thawDisplayRotation", int.class);
    } catch (Exception ignored) {
    }
  }

  public static void freezeRotation(int displayId, int rotation) {
    try {
      if (freezeDisplayRotationMethod == null) throw new Exception("no display");
      freezeDisplayRotationMethod.invoke(manager, displayId, rotation);
    } catch (Exception ignored) {
      try {
        if (freezeRotationMethod != null && displayId == Display.DEFAULT_DISPLAY) freezeRotationMethod.invoke(manager, rotation);
      } catch (Exception ignored1) {
      }
    }
  }

  public static boolean isRotationFrozen(int displayId) {
    try {
      if (isDisplayRotationFrozenMethod != null) throw new Exception("no display");
      return (boolean) isDisplayRotationFrozenMethod.invoke(manager, displayId);
    } catch (Exception ignored) {
      try {
        if (isRotationFrozenMethod != null && displayId == Display.DEFAULT_DISPLAY) return (boolean) isRotationFrozenMethod.invoke(manager);
        return false;
      } catch (Exception ignored1) {
        return false;
      }
    }
  }

  public static void thawRotation(int displayId) {
    try {
      if (thawDisplayRotationMethod != null) throw new Exception("no display");
      thawDisplayRotationMethod.invoke(manager, displayId);
    } catch (Exception ignored) {
      try {
        if (thawRotationMethod != null && displayId == Display.DEFAULT_DISPLAY) thawRotationMethod.invoke(manager);
      } catch (Exception ignored1) {
      }
    }
  }

  public static void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
    try {
      try {
        CLASS.getMethod("watchRotation", IRotationWatcher.class, int.class).invoke(manager, rotationWatcher, displayId);
      } catch (NoSuchMethodException e) {
        CLASS.getMethod("watchRotation", IRotationWatcher.class).invoke(manager, rotationWatcher);
      }
    } catch (Exception ignored) {
    }
  }

}
