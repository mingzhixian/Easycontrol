package top.saymzx.scrcpy.server.wrappers

import android.view.Display
import top.saymzx.scrcpy.server.entity.DisplayInfo
import top.saymzx.scrcpy.server.helper.Command.execReadOutput
import java.util.regex.Pattern

class DisplayManager( // instance of hidden class android.hardware.display.DisplayManagerGlobal
  private val manager: Any
) {
  fun getDisplayInfo(displayId: Int): DisplayInfo? {
    return try {
      val displayInfo =
        manager.javaClass.getMethod("getDisplayInfo", Int::class.javaPrimitiveType).invoke(
          manager, displayId
        )
          ?: // fallback when displayInfo is null
          return getDisplayInfoFromDumpsysDisplay(displayId)
      val cls: Class<*> = displayInfo.javaClass
      // width and height already take the rotation into account
      val width = cls.getDeclaredField("logicalWidth").getInt(displayInfo)
      val height = cls.getDeclaredField("logicalHeight").getInt(displayInfo)
      val rotation = cls.getDeclaredField("rotation").getInt(displayInfo)
      val layerStack = cls.getDeclaredField("layerStack").getInt(displayInfo)
      val flags = cls.getDeclaredField("flags").getInt(displayInfo)
      DisplayInfo(displayId, Pair(width, height), rotation, layerStack, flags)
    } catch (e: Exception) {
      throw AssertionError(e)
    }
  }

  val displayIds: IntArray
    get() = try {
      manager.javaClass.getMethod("getDisplayIds").invoke(manager) as IntArray
    } catch (e: Exception) {
      throw AssertionError(e)
    }

  companion object {
    // public to call it from unit tests
    fun parseDisplayInfo(dumpsysDisplayOutput: String?, displayId: Int): DisplayInfo? {
      val regex = Pattern.compile(
        "^    mOverrideDisplayInfo=DisplayInfo\\{\".*?, displayId " + displayId + ".*?(, FLAG_.*)?, real ([0-9]+) x ([0-9]+).*?, "
            + "rotation ([0-9]+).*?, layerStack ([0-9]+)",
        Pattern.MULTILINE
      )
      val m = regex.matcher(dumpsysDisplayOutput)
      if (!m.find()) {
        return null
      }
      val flags = parseDisplayFlags(m.group(1))
      val width = m.group(2).toInt()
      val height = m.group(3).toInt()
      val rotation = m.group(4).toInt()
      val layerStack = m.group(5).toInt()
      return DisplayInfo(displayId, Pair(width, height), rotation, layerStack, flags)
    }

    private fun getDisplayInfoFromDumpsysDisplay(displayId: Int): DisplayInfo? {
      return try {
        val dumpsysDisplayOutput = execReadOutput("dumpsys", "display")
        parseDisplayInfo(dumpsysDisplayOutput, displayId)
      } catch (e: Exception) {
        null
      }
    }

    private fun parseDisplayFlags(text: String?): Int {
      val regex = Pattern.compile("FLAG_[A-Z_]+")
      if (text == null) {
        return 0
      }
      var flags = 0
      val m = regex.matcher(text)
      while (m.find()) {
        val flagString = m.group()
        try {
          val filed = Display::class.java.getDeclaredField(flagString)
          flags = flags or filed.getInt(null)
        } catch (e: NoSuchFieldException) {
          // Silently ignore, some flags reported by "dumpsys display" are @TestApi
        } catch (e: IllegalAccessException) {
        }
      }
      return flags
    }
  }
}