/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.IRotationWatcher;

public final class WindowManager {
  private static IInterface manager;

  public static void init(IInterface m) throws NoSuchMethodException {
    manager = m;
  }

  public static void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
    try {
      Class<?> cls = manager.getClass();
      try {
        cls.getMethod("watchRotation", IRotationWatcher.class, int.class).invoke(manager, rotationWatcher, displayId);
      } catch (NoSuchMethodException e) {
        cls.getMethod("watchRotation", IRotationWatcher.class).invoke(manager, rotationWatcher);
      }
    } catch (Exception ignored) {
    }
  }

}
