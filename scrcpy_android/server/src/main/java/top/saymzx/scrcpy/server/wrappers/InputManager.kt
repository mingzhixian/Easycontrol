package top.saymzx.scrcpy.server.wrappers

import android.view.InputEvent
import android.view.MotionEvent
import java.lang.reflect.Method

class InputManager(private val manager: Any) {
  @get:Throws(NoSuchMethodException::class)
  private var injectInputEventMethod: Method = manager.javaClass.getMethod(
    "injectInputEvent",
    InputEvent::class.java,
    Int::class.javaPrimitiveType
  )

  fun injectInputEvent(inputEvent: InputEvent?, mode: Int): Boolean {
    return try {
      val method = injectInputEventMethod
      method.invoke(manager, inputEvent, mode) as Boolean
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1
    const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2

    @get:Throws(NoSuchMethodException::class)
    private var setDisplayIdMethod: Method? = null
      get() {
        if (field == null) {
          field = InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        }
        return field
      }

    @get:Throws(NoSuchMethodException::class)
    private var setActionButtonMethod: Method? = null
      get() {
        if (field == null) {
          field = MotionEvent::class.java.getMethod("setActionButton", Int::class.javaPrimitiveType)
        }
        return field
      }

    fun setDisplayId(inputEvent: InputEvent?, displayId: Int): Boolean {
      return try {
        val method = setDisplayIdMethod
        method!!.invoke(inputEvent, displayId)
        true
      } catch (e: Exception) {
        false
      }
    }

    fun setActionButton(motionEvent: MotionEvent?, actionButton: Int): Boolean {
      return try {
        val method = setActionButtonMethod
        method!!.invoke(motionEvent, actionButton)
        true
      } catch (e: Exception) {
        false
      }
    }
  }
}