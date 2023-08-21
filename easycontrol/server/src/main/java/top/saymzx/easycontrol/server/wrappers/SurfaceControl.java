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

  private static Method getBuiltInDisplayMethod;
  private static Method setDisplayPowerModeMethod;
  private static Method getPhysicalDisplayTokenMethod;
  private static Method getPhysicalDisplayIdsMethod;

  public static void init() throws ClassNotFoundException, NoSuchMethodException {
    CLASS = Class.forName("android.view.SurfaceControl");
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      getBuiltInDisplayMethod = CLASS.getMethod("getBuiltInDisplay", int.class);
    } else {
      getBuiltInDisplayMethod = CLASS.getMethod("getInternalDisplayToken");
    }
    getPhysicalDisplayTokenMethod = CLASS.getMethod("getPhysicalDisplayToken", long.class);
    getPhysicalDisplayIdsMethod = CLASS.getMethod("getPhysicalDisplayIds");
    setDisplayPowerModeMethod = CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class);
  }

  public static void openTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("openTransaction").invoke(null);
  }

  public static void closeTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("closeTransaction").invoke(null);
  }

  public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class)
        .invoke(null, displayToken, orientation, layerStackRect, displayRect);
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
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return (IBinder) getBuiltInDisplayMethod.invoke(null, 0);
      }
      return (IBinder) getBuiltInDisplayMethod.invoke(null);
    } catch (InvocationTargetException | IllegalAccessException e) {
      return null;
    }
  }

  public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
    try {
      return (IBinder) getPhysicalDisplayTokenMethod.invoke(null, physicalDisplayId);
    } catch (InvocationTargetException | IllegalAccessException e) {
      return null;
    }
  }

  public static long[] getPhysicalDisplayIds() {
    try {
      return (long[]) getPhysicalDisplayIdsMethod.invoke(null);
    } catch (InvocationTargetException | IllegalAccessException e) {
      return null;
    }
  }

  public static boolean setDisplayPowerMode(IBinder displayToken, int mode) {
    try {
      setDisplayPowerModeMethod.invoke(null, displayToken, mode);
      return true;
    } catch (InvocationTargetException | IllegalAccessException e) {
      return false;
    }
  }

}
