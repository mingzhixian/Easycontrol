/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class SurfaceControl {

  private static Class<?> CLASS;

  private static Method getBuiltInDisplayMethod = null;
  private static Method setDisplayPowerModeMethod = null;
  private static Method getPhysicalDisplayTokenMethod = null;
  private static Method getPhysicalDisplayIdsMethod = null;

  public static void init() throws ClassNotFoundException {
    CLASS = Class.forName("android.view.SurfaceControl");
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
          getPhysicalDisplayIdsMethod = CLASS.getMethod("getPhysicalDisplayIds");
          getPhysicalDisplayTokenMethod = CLASS.getMethod("getPhysicalDisplayToken", long.class);
        } catch (Exception ignored) {
          getMethodAndroid14();
        }
      }
      setDisplayPowerModeMethod = CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class);
    } catch (Exception ignored) {
    }
  }

  // 安卓14之后部分函数转移到了DisplayControl
  @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "BlockedPrivateApi"})
  private static void getMethodAndroid14() throws Exception {
    Class<?> displayControlClass = null;
    try {
      Method createClassLoaderMethod = Class.forName("com.android.internal.os.ClassLoaderFactory").getDeclaredMethod("createClassLoader", String.class, String.class, String.class, ClassLoader.class, int.class, boolean.class, String.class);
      ClassLoader classLoader = (ClassLoader) createClassLoaderMethod.invoke(null, "/system/framework/services.jar", null, null, ClassLoader.getSystemClassLoader(), 0, true, null);
      displayControlClass = classLoader.loadClass("com.android.server.display.DisplayControl");
//      Method loadMethod = Runtime.class.getDeclaredMethod("loadLibrary0", Class.class, String.class);
//      loadMethod.setAccessible(true);
//      if ((Build.BRAND.toLowerCase() + Build.MANUFACTURER.toLowerCase()).contains("honor")) throw new Exception("Honor device");
//      loadMethod.invoke(Runtime.getRuntime(), displayControlClass, "android_servers");
    } catch (Throwable ignored) {
    }
    if (displayControlClass == null) throw new Exception("Failed to load DisplayControl class");
    getPhysicalDisplayIdsMethod = displayControlClass.getMethod("getPhysicalDisplayIds");
    getPhysicalDisplayTokenMethod = displayControlClass.getMethod("getPhysicalDisplayToken", long.class);
  }

  public static void openTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("openTransaction").invoke(null);
  }

  public static void closeTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("closeTransaction").invoke(null);
  }

  public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class).invoke(null, displayToken, orientation, layerStackRect, displayRect);
  }

  public static void setDisplayLayerStack(IBinder displayToken, int layerStack) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("setDisplayLayerStack", IBinder.class, int.class).invoke(null, displayToken, layerStack);
  }

  public static void setDisplaySurface(IBinder displayToken, Surface surface) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("setDisplaySurface", IBinder.class, Surface.class).invoke(null, displayToken, surface);
  }

  public static IBinder createDisplay(String name, boolean secure) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return (IBinder) CLASS.getMethod("createDisplay", String.class, boolean.class).invoke(null, name, secure);
  }

  public static void destroyDisplay(IBinder displayToken) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("destroyDisplay", IBinder.class).invoke(null, displayToken);
  }

  public static IBinder getBuiltInDisplay() {
    try {
      if (getBuiltInDisplayMethod == null) getBuiltInDisplayMethod = CLASS.getMethod("getBuiltInDisplay", int.class);
      return (IBinder) getBuiltInDisplayMethod.invoke(null, 0);
    } catch (Exception ignored) {
      return null;
    }
  }

  public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
    if (getPhysicalDisplayTokenMethod == null) return null;
    try {
      return (IBinder) getPhysicalDisplayTokenMethod.invoke(null, physicalDisplayId);
    } catch (Exception ignored) {
      return null;
    }
  }

  public static long[] getPhysicalDisplayIds() {
    if (getPhysicalDisplayIdsMethod == null) return null;
    try {
      return (long[]) getPhysicalDisplayIdsMethod.invoke(null);
    } catch (Exception ignored) {
      return null;
    }
  }

  public static void setDisplayPowerMode(IBinder displayToken, int mode) {
    if (setDisplayPowerModeMethod == null) return;
    try {
      setDisplayPowerModeMethod.invoke(null, displayToken, mode);
    } catch (Exception ignored) {
    }
  }

}
