package top.saymzx.scrcpy.server.helper

import okio.BufferedSource
import top.saymzx.scrcpy.server.Server.isNormal
import top.saymzx.scrcpy.server.entity.Device

object Controller {
  lateinit var controlStreamIn: BufferedSource


  fun handle(): Thread {
    val thread = Thread {
      try {
        while (isNormal) {
          when (controlStreamIn.readByte().toInt()) {
            0 -> handleTouchEvent()
            1 -> handleKeyEvent()
            2 -> handleClipboardEvent()
            3 -> handleSetScreenPowerModeEvent()
          }
        }
      } catch (e: Exception) {
        print(e)
        isNormal = false
      }
    }
    thread.priority = Thread.MAX_PRIORITY
    return thread
  }

  private fun handleTouchEvent() {
    val action = controlStreamIn.readInt()
    val pointerId = controlStreamIn.readInt()
    val position = Pair(controlStreamIn.readInt(), controlStreamIn.readInt())
    val size = Pair(controlStreamIn.readInt(), controlStreamIn.readInt())
    Device.touchEvent(action, position, size, pointerId)
  }

  private fun handleKeyEvent() {
    val action = controlStreamIn.readInt()
    val keyCode = controlStreamIn.readInt()
    Device.keyEvent(action, keyCode)
  }

  private fun handleClipboardEvent() {
    val size = controlStreamIn.readInt()
    val text = String(controlStreamIn.readByteArray(size.toLong()))
    Device.setClipboardText(text)
  }

  private fun handleSetScreenPowerModeEvent() {
    Device.setScreenPowerMode(controlStreamIn.readByte().toInt())
  }
}