package top.saymzx.scrcpy.server.wrappers

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.IInterface

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
object ServiceManager {
  private var GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager")
    .getDeclaredMethod("getService", String::class.java)

  var windowManager = WindowManager(getService("window", "android.view.IWindowManager"))
  var displayManager = DisplayManager(
    Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredMethod("getInstance")
      .invoke(null)!!
  )
  var inputManager =
    InputManager(inputManagerClass.getDeclaredMethod("getInstance").invoke(null)!!)
  var powerManager = PowerManager(getService("power", "android.os.IPowerManager"))

  var clipboardManager = ClipboardManager(getService("clipboard", "android.content.IClipboard"))

  private fun getService(service: String, type: String): IInterface {
    return try {
      val binder = GET_SERVICE_METHOD.invoke(null, service) as IBinder
      val asInterfaceMethod =
        Class.forName("$type\$Stub").getMethod("asInterface", IBinder::class.java)
      asInterfaceMethod.invoke(null, binder) as IInterface
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  // Parts of the InputManager class have been moved to a new InputManagerGlobal class in Android 14 preview
  private val inputManagerClass: Class<*>
    get() = try {
      // Parts of the InputManager class have been moved to a new InputManagerGlobal class in Android 14 preview
      Class.forName("android.hardware.input.InputManagerGlobal")
    } catch (e: ClassNotFoundException) {
      android.hardware.input.InputManager::class.java
    }
}