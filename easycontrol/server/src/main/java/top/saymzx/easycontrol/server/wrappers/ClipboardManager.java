/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.content.ClipData;
import android.content.IOnPrimaryClipChangedListener;
import android.os.Build;
import android.os.IInterface;

import java.lang.reflect.Method;

import top.saymzx.easycontrol.server.helper.FakeContext;

public class ClipboardManager {
  private static IInterface manager;
  private static Method getPrimaryClipMethod = null;
  private static Method setPrimaryClipMethod = null;
  private static Method addPrimaryClipChangedListener = null;
  private static int getMethodVersion;
  private static int setMethodVersion;
  private static int addListenerMethodVersion;

  public static void init(IInterface m) {
    manager = m;
    if (manager == null) return;
    try {
      getGetPrimaryClipMethod();
      getSetPrimaryClipMethod();
      getAddPrimaryClipChangedListenerMethod();
    } catch (Exception ignored) {
    }
  }

  private static void getGetPrimaryClipMethod() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class);
    } else {
      for (int i = 0; i < 4; i++) {
        try {
          getMethodVersion = i;
          switch (i) {
            case 0:
              getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, int.class);
              return;
            case 1:
              getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class);
              return;
            case 2:
              getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class, int.class);
              return;
            case 3:
              getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, int.class, String.class);
              return;
          }
        } catch (Exception ignored) {
        }
      }
    }
  }

  private static void getSetPrimaryClipMethod() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class);
    } else {
      for (int i = 0; i < 3; i++) {
        try {
          setMethodVersion = i;
          switch (i) {
            case 0:
              setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, int.class);
              return;
            case 1:
              setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class);
              return;
            case 2:
              setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class, int.class);
              return;
          }
        } catch (Exception ignored) {
        }
      }
    }
  }

  private static void getAddPrimaryClipChangedListenerMethod() throws NoSuchMethodException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class);
    else {
      for (int i = 0; i < 3; i++) {
        try {
          addListenerMethodVersion = i;
          switch (i) {
            case 0:
              addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, int.class);
              return;
            case 1:
              addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class, int.class);
              return;
            case 2:
              addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class, int.class, int.class);
              return;
          }
        } catch (Exception ignored) {
        }
      }
    }
  }

  public static void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
    if (addPrimaryClipChangedListener == null) return;
    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME);
      else {
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
    } catch (Exception ignored) {
    }
  }

  public static String getText() {
    if (getPrimaryClipMethod == null) return null;
    try {
      ClipData clipData;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME);
      else {
        switch (getMethodVersion) {
          case 0:
            clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
            break;
          case 1:
            clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
            break;
          case 2:
            clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
            break;
          default:
            clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID, null);
            break;
        }
      }
      if (clipData == null) return null;
      if (clipData.getItemCount() == 0) return null;
      return String.valueOf(clipData.getItemAt(0).getText());
    } catch (Exception e) {
      return null;
    }
  }

  public static void setText(String text) {
    if (setPrimaryClipMethod == null) return;
    ClipData clipData = ClipData.newPlainText("easycontrol", text);
    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME);
      else {
        switch (setMethodVersion) {
          case 0:
            setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
            break;
          case 1:
            setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
            break;
          default:
            setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
            break;
        }
      }
    } catch (Exception ignored) {
    }
  }
}
