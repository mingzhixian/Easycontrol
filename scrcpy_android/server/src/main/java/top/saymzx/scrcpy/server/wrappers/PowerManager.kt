package top.saymzx.scrcpy.server.wrappers

import android.os.IInterface
import java.lang.reflect.Method

class PowerManager(private val manager: IInterface) {
  // we may lower minSdkVersion in the future
  @get:Throws(NoSuchMethodException::class)
  private var isScreenOnMethod: Method = manager.javaClass.getMethod("isInteractive")

  val isScreenOn: Boolean
    get() = try {
      val method = isScreenOnMethod
      method.invoke(manager) as Boolean
    } catch (e: Exception) {
      false
    }
}