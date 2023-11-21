/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.IRotationWatcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WindowManager {
  private static IInterface manager;
  private static Class<?> CLASS;
  private static Method freezeRotationMethod;
  private static Method isRotationFrozenMethod;
  private static Method thawRotationMethod;

  public static void init(IInterface m) throws NoSuchMethodException {
    manager = m;
    CLASS = manager.getClass();
    freezeRotationMethod = manager.getClass().getMethod("freezeRotation", int.class);
    isRotationFrozenMethod = manager.getClass().getMethod("isRotationFrozen");
    thawRotationMethod = manager.getClass().getMethod("thawRotation");
  }

  public static void freezeRotation(int rotation) {
    try {
      freezeRotationMethod.invoke(manager, rotation);
    } catch (InvocationTargetException | IllegalAccessException ignored) {
    }
  }

  public static boolean isRotationFrozen() {
    try {
      return (boolean) isRotationFrozenMethod.invoke(manager);
    } catch (InvocationTargetException | IllegalAccessException ignored) {
      return false;
    }
  }

  public static void thawRotation() {
    try {
      thawRotationMethod.invoke(manager);
    } catch (InvocationTargetException | IllegalAccessException ignored) {
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
