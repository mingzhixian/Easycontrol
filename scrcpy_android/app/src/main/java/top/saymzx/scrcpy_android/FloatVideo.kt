package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.MediaFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@SuppressLint("CutPasteId", "ClickableViewAccessibility")
class FloatVideo(private val main: MainActivity, private val scrcpy: Scrcpy) {

  // 悬浮窗
  @SuppressLint("InflateParams")
  val floatVideo: View = LayoutInflater.from(main).inflate(R.layout.float_video, null)
  private lateinit var floatVideoParams: WindowManager.LayoutParams

  // 悬浮窗大小位置
  private var floatVideoWidth = 0
  private var floatVideoHeight = 0

  // 视频大小
  private var localVideoWidth = 0
  private var localVideoHeight = 0
  var remoteVideoWidth = 0
  var remoteVideoHeight = 0

  // 系统分辨率
  private var deviceWidth = 0
  private var deviceHeight = 0

  init {
    // 获取设备真是分辨率
    val metric = DisplayMetrics()
    main.windowManager.defaultDisplay.getRealMetrics(metric)
    deviceWidth = metric.widthPixels
    deviceHeight = metric.heightPixels
    // 初始化悬浮窗大小位置信息
    floatVideoWidth = deviceWidth
    floatVideoHeight = deviceHeight
    localVideoWidth = floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).width
    localVideoHeight = floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).height
    // 关闭按钮动作监听
    floatVideo.findViewById<ImageView>(R.id.float_video_stop).setOnClickListener {
      hide()
    }
    // 导航按钮监听
    floatVideo.findViewById<ImageView>(R.id.float_video_back).setOnClickListener {
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener {
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener {
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_APP_SWITCH)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_APP_SWITCH)
    }
    // 横条按钮监听
    val floatVideoBarGestureDetector =
      GestureDetector(main, object : GestureDetector.SimpleOnGestureListener() {
      })
    floatVideo.findViewById<View>(R.id.float_video_bar).setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          floatVideoBarGestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
        MotionEvent.ACTION_MOVE -> {
          event.action = MotionEvent.ACTION_CANCEL
          floatVideoBarGestureDetector.onTouchEvent(event)
          // 移动悬浮窗(非全屏状态)
          if (!scrcpy.device.isFull) {
            floatVideoParams.x = event.rawX.toInt()
            floatVideoParams.y = event.rawY.toInt()
            main.windowManager.updateViewLayout(floatVideo, floatVideoParams)
          }
          return@setOnTouchListener true
        }
        else -> {
          floatVideoBarGestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
      }
    }
    // 悬浮窗大小修改监听
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener { _, event ->
      if (event.actionMasked == MotionEvent.ACTION_MOVE) {
        event.action = MotionEvent.ACTION_CANCEL
        floatVideoBarGestureDetector.onTouchEvent(event)
        val newWidth = event.rawX.toInt() - floatVideoParams.x
        val newHeight = event.rawY.toInt() - floatVideoParams.y
        setSize(newWidth, newHeight, scrcpy.device.setResolution)
      }
      return@setOnTouchListener true
    }
    // 视频界面控制
    val pointerList = ArrayList<Int>(20)
    for (i in 1..20) pointerList.add(0)
    floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).setOnTouchListener { _, event ->
      try {
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            packTouchControl(
              MotionEvent.ACTION_DOWN,
              p,
              x * remoteVideoWidth / localVideoWidth,
              y * remoteVideoHeight / localVideoHeight
            )
            // 记录xy信息
            pointerList[p] = x
            pointerList[10 + p] = y
          }
          MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            packTouchControl(
              MotionEvent.ACTION_UP,
              p,
              x * remoteVideoWidth / localVideoWidth,
              y * remoteVideoHeight / localVideoHeight
            )
          }
          MotionEvent.ACTION_MOVE -> {
            for (i in 0 until event.pointerCount) {
              val x = event.getX(i).toInt()
              val y = event.getY(i).toInt()
              val p = event.getPointerId(i)
              // 适配一些机器将点击视作小范围移动(小于3的圆内不做处理)
              if (pointerList[p] != -1) {
                if ((pointerList[p] - x) * (pointerList[p] - x) + (pointerList[10 + p] - y) * (pointerList[10 + p] - y) < 9) return@setOnTouchListener true
                pointerList[p] = -1
              }
              packTouchControl(
                MotionEvent.ACTION_UP,
                p,
                x * remoteVideoWidth / localVideoWidth,
                y * remoteVideoHeight / localVideoHeight
              )
            }
          }
        }
      } catch (_: IllegalArgumentException) {
      }
      return@setOnTouchListener true
    }
  }

  // 显示悬浮窗
  fun show() {
    floatVideoParams = WindowManager.LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
      flags =
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON      //位置大小设置
      gravity = Gravity.START or Gravity.TOP
      x = 0
      y = 0
      width = floatVideoWidth
      height = floatVideoHeight
    }
    // 显示悬浮窗
    main.windowManager.addView(floatVideo, floatVideoParams)
  }

  // 隐藏悬浮窗
  fun hide(){
    main.windowManager.removeView(floatVideo)
    scrcpy.stop()
  }

  // 组装触控报文
  private fun packTouchControl(action: Int, p: Int, x: Int, y: Int) {
    val touchByteBuffer = ByteBuffer.allocate(32)
    touchByteBuffer.clear()
    // 触摸事件
    touchByteBuffer.put(2)
    // 触摸类型
    touchByteBuffer.put(action.toByte())
    // pointerId
    touchByteBuffer.putLong(p.toLong())
    // 坐标位置
    touchByteBuffer.putInt(x)
    touchByteBuffer.putInt(y)
    // 屏幕尺寸
    touchByteBuffer.putShort(localVideoWidth.toShort())
    touchByteBuffer.putShort(localVideoHeight.toShort())
    // 按压力度presureInt和buttons
    touchByteBuffer.putShort(1)
    touchByteBuffer.putInt(0)
    touchByteBuffer.putInt(0)
    touchByteBuffer.flip()
    scrcpy.controls.offer(touchByteBuffer.array())
  }

  // 组装导航报文
  private fun packNavControl(action: Int, key: Int) {
    val navByteBuffer = ByteBuffer.allocate(14)
    navByteBuffer.clear()
    // 输入事件
    navByteBuffer.put(0)
    // 事件类型
    navByteBuffer.put(action.toByte())
    // 按键类型
    navByteBuffer.putInt(key)
    // 重复次数
    navByteBuffer.putInt(0)
    navByteBuffer.putInt(0)
    navByteBuffer.flip()
    scrcpy.controls.offer(navByteBuffer.array())
  }

  // 判断是否旋转
  fun isRotation(format: MediaFormat) {
    val newWidth = format.getInteger("width")
    val newHeight = format.getInteger("height")
    // 检测是否旋转
    if ((newWidth > newHeight) xor (localVideoWidth > localVideoHeight)) {
      setSize(newWidth, newHeight, false)
      val tmp = remoteVideoWidth
      remoteVideoWidth = remoteVideoHeight
      remoteVideoHeight = tmp
      // 旋转方向
      main.requestedOrientation =
        if (newWidth > newHeight) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }

  // 修改悬浮窗大小
  private fun setSize(width: Int, height: Int, isSetResolution: Boolean) {
    // 更新悬浮窗大小
    floatVideoWidth = width
    floatVideoHeight = height
    floatVideoParams.width = width
    floatVideoParams.height = height
    main.windowManager.updateViewLayout(floatVideo, floatVideoParams)
    // 更新视频界面大小
    localVideoWidth = floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).width
    localVideoHeight = floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).height
    // 修改分辨率
    if (isSetResolution) {
      scrcpy.coroutineScope.launch {
        scrcpy.runAdbCmd("wm size $localVideoWidth x $localVideoHeight")
      }
    }
  }
}