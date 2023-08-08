package top.saymzx.scrcpy.server.wrappers

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Surface
import java.lang.reflect.InvocationTargetException

@SuppressLint("PrivateApi")
object SurfaceControl {
  private var CLASS = Class.forName("android.view.SurfaceControl")

  const val POWER_MODE_OFF = 0
  const val POWER_MODE_NORMAL = 2

  private var getBuiltInDisplayMethod = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
    CLASS.getMethod("getBuiltInDisplay", Int::class.javaPrimitiveType)
  } else {
    CLASS.getMethod("getInternalDisplayToken")
  }

  private var setDisplayPowerModeMethod = CLASS.getMethod(
    "setDisplayPowerMode",
    IBinder::class.java,
    Int::class.javaPrimitiveType
  )

  private var getPhysicalDisplayTokenMethod =
    CLASS.getMethod("getPhysicalDisplayToken", Long::class.javaPrimitiveType)

  private var getPhysicalDisplayIdsMethod = CLASS.getMethod("getPhysicalDisplayIds")

  fun openTransaction() {
    try {
      CLASS.getMethod("openTransaction").invoke(null)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  fun closeTransaction() {
    try {
      CLASS.getMethod("closeTransaction").invoke(null)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  fun setDisplayProjection(
    displayToken: IBinder?,
    orientation: Int,
    layerStackRect: Rect?,
    displayRect: Rect?
  ) {
    try {
      CLASS.getMethod(
        "setDisplayProjection",
        IBinder::class.java,
        Int::class.javaPrimitiveType,
        Rect::class.java,
        Rect::class.java
      )
        .invoke(null, displayToken, orientation, layerStackRect, displayRect)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  fun setDisplayLayerStack(displayToken: IBinder?, layerStack: Int) {
    try {
      CLASS.getMethod("setDisplayLayerStack", IBinder::class.java, Int::class.javaPrimitiveType)
        .invoke(null, displayToken, layerStack)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  fun setDisplaySurface(displayToken: IBinder?, surface: Surface?) {
    try {
      CLASS.getMethod("setDisplaySurface", IBinder::class.java, Surface::class.java)
        .invoke(null, displayToken, surface)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  fun createDisplay(name: String?, secure: Boolean): IBinder {
    return try {
      CLASS.getMethod("createDisplay", String::class.java, Boolean::class.javaPrimitiveType)
        .invoke(null, name, secure) as IBinder
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }
  // call getBuiltInDisplay(0)

  // call getInternalDisplayToken()
  val builtInDisplay: IBinder?
    get() = try {
      val method = getBuiltInDisplayMethod
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // call getBuiltInDisplay(0)
        method.invoke(null, 0) as IBinder
      } else method.invoke(null) as IBinder

      // call getInternalDisplayToken()
    } catch (e: InvocationTargetException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    }

  fun getPhysicalDisplayToken(physicalDisplayId: Long): IBinder? {
    return try {
      val method = getPhysicalDisplayTokenMethod
      method.invoke(null, physicalDisplayId) as IBinder
    } catch (e: InvocationTargetException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    }
  }

  val physicalDisplayIds: LongArray?
    get() = try {
      val method = getPhysicalDisplayIdsMethod
      method.invoke(null) as LongArray
    } catch (e: InvocationTargetException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    }

  fun setDisplayPowerMode(displayToken: IBinder?, mode: Int): Boolean {
    return try {
      val method = setDisplayPowerModeMethod
      method.invoke(null, displayToken, mode)
      true
    } catch (e: InvocationTargetException) {
      false
    } catch (e: IllegalAccessException) {
      false
    } catch (e: NoSuchMethodException) {
      false
    }
  }

  fun destroyDisplay(displayToken: IBinder?) {
    try {
      CLASS.getMethod("destroyDisplay", IBinder::class.java).invoke(null, displayToken)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }
}