package top.saymzx.easycontrol.server.wrappers;

import android.content.ClipData;
import android.content.IOnPrimaryClipChangedListener;
import android.os.Build;
import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import top.saymzx.easycontrol.server.helper.FakeContext;

public class ClipboardManager {
  private static IInterface manager;
  private static Method getPrimaryClipMethod;
  private static Method setPrimaryClipMethod;
  private static Method addPrimaryClipChangedListener;
  private static int getMethodVersion;
  private static int setMethodVersion;
  private static int addListenerMethodVersion;

  public static void init(IInterface m) throws NoSuchMethodException {
    manager = m;
    getPrimaryClipMethod = getGetPrimaryClipMethod();
    setPrimaryClipMethod = getSetPrimaryClipMethod();
    addPrimaryClipChangedListener = getAddPrimaryClipChangedListener();
  }

  private static Method getGetPrimaryClipMethod() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return manager.getClass().getMethod("getPrimaryClip", String.class);
    } else {
      try {
        getMethodVersion = 0;
        return manager.getClass().getMethod("getPrimaryClip", String.class, int.class);
      } catch (NoSuchMethodException e1) {
        try {
          getMethodVersion = 1;
          return manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class);
        } catch (NoSuchMethodException e2) {
          try {
            getMethodVersion = 2;
            return manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class, int.class);
          } catch (NoSuchMethodException e3) {
            getMethodVersion = 3;
            return manager.getClass().getMethod("getPrimaryClip", String.class, int.class, String.class);
          }
        }
      }
    }
  }

  private static Method getSetPrimaryClipMethod() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class);
    } else {
      try {
        setMethodVersion = 0;
        return manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, int.class);
      } catch (NoSuchMethodException e1) {
        try {
          setMethodVersion = 1;
          return manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class);
        } catch (NoSuchMethodException e2) {
          setMethodVersion = 2;
          return manager.getClass()
              .getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class, int.class);
        }
      }
    }
  }

  private static Method getAddPrimaryClipChangedListener() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return manager.getClass()
          .getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class);
    } else {
      try {
        addListenerMethodVersion = 0;
        return manager.getClass()
            .getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, int.class);
      } catch (NoSuchMethodException e1) {
        try {
          addListenerMethodVersion = 1;
          return manager.getClass()
              .getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class,
                  int.class);
        } catch (NoSuchMethodException e2) {
          addListenerMethodVersion = 2;
          return manager.getClass()
              .getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class,
                  int.class, int.class);
        }
      }
    }
  }

  public static CharSequence getText() {
    try {
      ClipData clipData = getPrimaryClip(getPrimaryClipMethod, getMethodVersion, manager);
      if (clipData == null || clipData.getItemCount() == 0) {
        return null;
      }
      return clipData.getItemAt(0).getText();
    } catch (InvocationTargetException | IllegalAccessException e) {
      return null;
    }
  }

  public static void setText(CharSequence text) {
    try {
      ClipData clipData = ClipData.newPlainText(null, text);
      setPrimaryClip(setPrimaryClipMethod, setMethodVersion, manager, clipData);
    } catch (InvocationTargetException | IllegalAccessException ignored) {
    }
  }


  public static void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME);
      } else {
        switch (addListenerMethodVersion) {
          case 0:
            addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
            break;
          case 1:
            addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
            break;
          default:
            addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
            break;
        }
      }
    } catch (InvocationTargetException | IllegalAccessException ignored) {
    }
  }

  private static ClipData getPrimaryClip(Method method, int methodVersion, IInterface manager)
      throws InvocationTargetException, IllegalAccessException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return (ClipData) method.invoke(manager, FakeContext.PACKAGE_NAME);
    }

    switch (methodVersion) {
      case 0:
        return (ClipData) method.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
      case 1:
        return (ClipData) method.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
      case 2:
        return (ClipData) method.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
      default:
        return (ClipData) method.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID, null);
    }
  }

  private static void setPrimaryClip(Method method, int methodVersion, IInterface manager, ClipData clipData)
      throws InvocationTargetException, IllegalAccessException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      method.invoke(manager, clipData, FakeContext.PACKAGE_NAME);
      return;
    }

    switch (methodVersion) {
      case 0:
        method.invoke(manager, clipData, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
        break;
      case 1:
        method.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
        break;
      default:
        method.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
        break;
    }
  }
}
