package top.saymzx.scrcpy.server.entity

import android.annotation.SuppressLint
import android.content.IOnPrimaryClipChangedListener
import android.os.Build
import android.os.SystemClock
import android.view.IRotationWatcher
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import okio.BufferedSink
import top.saymzx.scrcpy.server.wrappers.InputManager
import top.saymzx.scrcpy.server.wrappers.ServiceManager
import top.saymzx.scrcpy.server.wrappers.SurfaceControl

object Device {

  var deviceSize: Pair<Int, Int>
  var devicePortrait: Boolean
  lateinit var videoSize: Pair<Int, Int>

  lateinit var controlStreamOut: BufferedSink

  lateinit var rotationListener: () -> Unit
  private var nowClipboardText = ""

  private const val displayId = 0
  val layerStack: Int

  const val POWER_MODE_OFF = SurfaceControl.POWER_MODE_OFF
  const val POWER_MODE_NORMAL = SurfaceControl.POWER_MODE_NORMAL
  const val INJECT_MODE_ASYNC = InputManager.INJECT_INPUT_EVENT_MODE_ASYNC
  const val INJECT_MODE_WAIT_FOR_RESULT = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT
  const val INJECT_MODE_WAIT_FOR_FINISH = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
  const val LOCK_VIDEO_ORIENTATION_UNLOCKED = -1
  const val LOCK_VIDEO_ORIENTATION_INITIAL = -2

  init {
    val displayInfo = ServiceManager.displayManager.getDisplayInfo(displayId)
    deviceSize = displayInfo!!.size
    devicePortrait = displayInfo.rotation == 0 || displayInfo.rotation == 3
    // 计算视频大小
    computeVideoSize()
    layerStack = displayInfo.layerStack
    // 旋转监听
    setRotationListener()
    // 剪切板监听
    setClipBoardListener()
  }

  private fun computeVideoSize() {
    // h264只接受8的倍数，所以需要缩放至最近参数
    val isPortrait = deviceSize.first < deviceSize.second
    var major: Int = if (isPortrait) deviceSize.second else deviceSize.first
    var minor: Int = if (isPortrait) deviceSize.first else deviceSize.second
    if (major > Options.maxSize) {
      val minorExact: Int = minor * Options.maxSize / major
      minor = (minorExact + 4) and 7.inv()
      major = Options.maxSize
    }
    videoSize = if (isPortrait) Pair(minor, major) else Pair(major, minor)
  }

  // 旋转监听
  private fun setRotationListener() {
    ServiceManager.windowManager.registerRotationWatcher(object : IRotationWatcher.Stub() {
      override fun onRotationChanged(rotation: Int) {
        val newPortrait = rotation == 0 || rotation == 3
        if (newPortrait != devicePortrait) {
          deviceSize = Pair(deviceSize.second, deviceSize.first)
          rotationListener()
          // 发送报文
          controlStreamOut.writeByte(21)
        }
      }
    }, displayId)
  }

  // 剪切板监听
  private fun setClipBoardListener() {
    ServiceManager.clipboardManager.addPrimaryClipChangedListener(object :
      IOnPrimaryClipChangedListener.Stub() {
      override fun dispatchPrimaryClipChanged() {
        val newClipboardText = ServiceManager.clipboardManager.text.toString()
        if (newClipboardText != nowClipboardText) {
          // 发送报文
          if (newClipboardText.toByteArray().size > 5000) return
          nowClipboardText = newClipboardText
          val tmpTextByte = nowClipboardText.toByteArray()
          controlStreamOut.writeByte(20)
          controlStreamOut.writeInt(tmpTextByte.size)
          controlStreamOut.write(tmpTextByte)
        }
      }
    })
  }

  fun setClipboardText(text: String) {
    if (nowClipboardText != text) {
      nowClipboardText = text
      ServiceManager.clipboardManager.setText(nowClipboardText)
    }
  }

  private var lastTouchDown: Long = 0
  private val pointersState: PointersState = PointersState()
  private val pointerProperties = Array(10) { PointerProperties() }
  private val pointerCoords = Array(10) { PointerCoords() }

  @SuppressLint("Recycle")
  fun touchEvent(action: Int, position: Pair<Int, Int>, size: Pair<Int, Int>, pointerId: Int) {
    // 防止旋转中触摸
    if ((size.first > size.second) xor (deviceSize.first > deviceSize.second)) return
    pointerProperties[pointerId].id = pointerId
    pointerProperties[pointerId].toolType = MotionEvent.TOOL_TYPE_FINGER
    val now = SystemClock.uptimeMillis()
    val pointerIndex = pointersState.getPointerIndex(pointerId)
    if (pointerIndex == -1) {
      return
    }
    val pointer = pointersState[pointerIndex]
    pointer.x = position.first * deviceSize.first / size.first.toFloat()
    pointer.y = position.second * deviceSize.second / size.second.toFloat()

    pointer.isUp = action == MotionEvent.ACTION_UP

    val pointerCount = pointersState.update(pointerProperties, pointerCoords)
    var newAction = action
    if (pointerCount == 1) {
      if (action == MotionEvent.ACTION_DOWN) {
        lastTouchDown = now
      }
    } else {
      if (action == MotionEvent.ACTION_UP) {
        newAction =
          MotionEvent.ACTION_POINTER_UP or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      } else if (action == MotionEvent.ACTION_DOWN) {
        newAction =
          MotionEvent.ACTION_POINTER_DOWN or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      }
    }
    val event = MotionEvent
      .obtain(
        lastTouchDown,
        now,
        newAction,
        pointerCount,
        pointerProperties,
        pointerCoords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        InputDevice.SOURCE_TOUCHSCREEN,
        0
      )
    injectEvent(event)
  }

  fun keyEvent(
    action: Int,
    keyCode: Int,
  ) {
    val now = SystemClock.uptimeMillis()
    val event = KeyEvent(
      now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
      InputDevice.SOURCE_KEYBOARD
    )
    injectEvent(event)
  }

  private fun injectEvent(inputEvent: InputEvent) {
    try {
      ServiceManager.inputManager.injectInputEvent(inputEvent, INJECT_MODE_ASYNC)
    } catch (_: Exception) {
    }
  }

  fun setScreenPowerMode(mode: Int): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val physicalDisplayIds = SurfaceControl.physicalDisplayIds ?: return false
      var allOk = true
      for (physicalDisplayId in physicalDisplayIds) {
        val binder = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId)
        allOk = allOk and SurfaceControl.setDisplayPowerMode(binder, mode)
      }
      return allOk
    } else {
      val d = SurfaceControl.builtInDisplay ?: return false
      return SurfaceControl.setDisplayPowerMode(d, mode)
    }
  }
}