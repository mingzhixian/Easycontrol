package top.saymzx.scrcpy.server.wrappers

import android.os.IInterface
import android.view.IRotationWatcher
import java.lang.reflect.InvocationTargetException

class WindowManager( // old version
  private val manager: IInterface
) {

  private var getRotationMethod = try {
    manager.javaClass.getMethod("getDefaultDisplayRotation")
  } catch (e: NoSuchMethodException) {
    manager.javaClass.getMethod("getRotation")
  }

  private var freezeRotationMethod =
    manager.javaClass.getMethod("freezeRotation", Int::class.javaPrimitiveType)

  private var isRotationFrozenMethod = manager.javaClass.getMethod("isRotationFrozen")

  private var thawRotationMethod = manager.javaClass.getMethod("thawRotation")

  val rotation: Int
    get() = try {
      getRotationMethod.invoke(manager) as Int
    } catch (e: InvocationTargetException) {
      0
    } catch (e: IllegalAccessException) {
      0
    } catch (e: NoSuchMethodException) {
      0
    }

  fun freezeRotation(rotation: Int) {
    try {
      freezeRotationMethod.invoke(manager, rotation)
    } catch (_: InvocationTargetException) {
    } catch (_: IllegalAccessException) {
    } catch (_: NoSuchMethodException) {
    }
  }

  val isRotationFrozen: Boolean
    get() = try {
      isRotationFrozenMethod.invoke(manager) as Boolean
    } catch (e: InvocationTargetException) {
      false
    } catch (e: IllegalAccessException) {
      false
    } catch (e: NoSuchMethodException) {
      false
    }

  fun thawRotation() {
    try {
      thawRotationMethod.invoke(manager)
    } catch (_: InvocationTargetException) {
    } catch (_: IllegalAccessException) {
    } catch (_: NoSuchMethodException) {
    }
  }

  fun registerRotationWatcher(rotationWatcher: IRotationWatcher?, displayId: Int) {
    try {
      val cls: Class<*> = manager.javaClass
      try {
        cls.getMethod("watchRotation", IRotationWatcher::class.java, Int::class.javaPrimitiveType)
          .invoke(
            manager, rotationWatcher, displayId
          )
      } catch (e: NoSuchMethodException) {
        cls.getMethod("watchRotation", IRotationWatcher::class.java)
          .invoke(manager, rotationWatcher)
      }
    } catch (_: Exception) {
    }
  }
}