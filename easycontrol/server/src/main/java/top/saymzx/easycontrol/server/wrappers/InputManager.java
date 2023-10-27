/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.wrappers;

import android.view.InputEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InputManager {

  public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

  private static Object manager;
  private static Method injectInputEventMethod;

  public static void init(Object m) throws NoSuchMethodException {
    manager = m;
    injectInputEventMethod = manager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
  }


  public static void injectInputEvent(InputEvent inputEvent, int mode) throws InvocationTargetException, IllegalAccessException {
    injectInputEventMethod.invoke(manager, inputEvent, mode);
  }
}
