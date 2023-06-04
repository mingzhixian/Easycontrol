package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.view.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility", "InternalInsetResource", "DiscouragedApi")
class FloatVideo(
  private val scrcpy: Scrcpy,
  var remoteVideoWidth: Int,
  var remoteVideoHeight: Int
) {
  // 悬浮窗
  @SuppressLint("InflateParams")
  val floatVideo: View = LayoutInflater.from(scrcpy.main).inflate(R.layout.float_video, null)

  // 悬浮窗Layout
  private var floatVideoParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
    type =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else WindowManager.LayoutParams.TYPE_PHONE
    flags =
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    gravity = Gravity.START or Gravity.TOP
  }

  // 导航悬浮球
  @SuppressLint("InflateParams")
  lateinit var floatNav: View
  private lateinit var floatNavParams: WindowManager.LayoutParams

  // 视频大小
  private var localVideoWidth = 0
  private var localVideoHeight = 0

  // 控制队列
  val controls = LinkedList<ByteArray>() as Queue<ByteArray>

  // 显示悬浮窗
  fun show() {
    // 设置视频界面触摸监听
    setSurfaceListener()
    // 全屏or小窗模式
    scrcpy.main.windowManager.addView(floatVideo, floatVideoParams)
    if (scrcpy.device.isFull) setFull() else setSmallWindow()
    update(true)
  }

  // 隐藏悬浮窗
  fun hide() {
    scrcpy.main.windowManager.removeView(floatVideo)
    if (scrcpy.device.isFull && scrcpy.device.floatNav) scrcpy.main.windowManager.removeView(
      floatNav
    )
    scrcpy.main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  }

  // 更新悬浮窗
  private fun update(hasChangeSize: Boolean) {
    scrcpy.main.windowManager.updateViewLayout(
      floatVideo,
      floatVideoParams
    )
    if (scrcpy.device.isFull && scrcpy.device.floatNav) scrcpy.main.windowManager.updateViewLayout(
      floatNav,
      floatNavParams
    )
    // 减少未修改大小的无用调用
    if (hasChangeSize) {
      // 更新视频界面大小
      val surfaceView = floatVideo.findViewById<SurfaceView>(R.id.float_video_surface)
      surfaceView.doOnPreDraw {
        localVideoWidth = surfaceView.width
        localVideoHeight = surfaceView.height
      }
      // 小窗模式
      if (!scrcpy.device.isFull) {
        // 等比缩放控件大小
        val tmp =
          sqrt((floatVideoParams.width * floatVideoParams.height).toDouble() / (scrcpy.main.appData.deviceWidth * scrcpy.main.appData.deviceHeight).toDouble())
        // 整体背景圆角
        val floatVideoShape = floatVideo.background as GradientDrawable
        floatVideoShape.cornerRadius =
          scrcpy.main.resources.getDimension(R.dimen.floatVideoBackground) * tmp.toFloat()
        // 上空白
        val floatVideoTitle1 = floatVideo.findViewById<LinearLayout>(R.id.float_video_title1)
        val floatVideoTitle1LayoutParams = floatVideoTitle1.layoutParams
        floatVideoTitle1LayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoTitle1.layoutParams = floatVideoTitle1LayoutParams
        // 下空白
        val floatVideoTitle2 = floatVideo.findViewById<LinearLayout>(R.id.float_video_title2)
        val floatVideoTitle2LayoutParams = floatVideoTitle1.layoutParams
        floatVideoTitle2LayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoTitle2.layoutParams = floatVideoTitle2LayoutParams
        // 横条
        val floatVideoBar = floatVideo.findViewById<View>(R.id.float_video_bar)
        val floatVideoBarLayoutParams = floatVideoBar.layoutParams
        floatVideoBarLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoBar.layoutParams = floatVideoBarLayoutParams
        floatVideoBar.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoBarPadding) * tmp).toInt())
        // 停止按钮
        val floatVideoStop = floatVideo.findViewById<ImageView>(R.id.float_video_stop)
        val floatVideoStopLayoutParams = floatVideoStop.layoutParams
        floatVideoStopLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoStop.layoutParams = floatVideoStopLayoutParams
        floatVideoStop.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
        // 最近任务键
        val floatVideoSwitch = floatVideo.findViewById<ImageView>(R.id.float_video_switch)
        val floatVideoSwitchLayoutParams = floatVideoSwitch.layoutParams
        floatVideoSwitchLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoSwitch.layoutParams = floatVideoSwitchLayoutParams
        floatVideoSwitch.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
        // 桌面键
        val floatVideoHome = floatVideo.findViewById<ImageView>(R.id.float_video_home)
        val floatVideoHomeLayoutParams = floatVideoHome.layoutParams
        floatVideoHomeLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoHome.layoutParams = floatVideoHomeLayoutParams
        floatVideoHome.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
        // 返回键
        val floatVideoBack = floatVideo.findViewById<ImageView>(R.id.float_video_back)
        val floatVideoBackLayoutParams = floatVideoBack.layoutParams
        floatVideoBackLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoBack.layoutParams = floatVideoBackLayoutParams
        floatVideoBack.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
        // 修改大小按钮
        val floatVideoSetSize = floatVideo.findViewById<ImageView>(R.id.float_video_set_size)
        val floatVideoSetSizeLayoutParams = floatVideoSetSize.layoutParams
        floatVideoSetSizeLayoutParams.height =
          (scrcpy.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
        floatVideoSetSize.layoutParams = floatVideoSetSizeLayoutParams
        floatVideoSetSize.setPadding((scrcpy.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
      }
    }
  }

  // 设置全屏
  private fun setFull() {
    scrcpy.device.isFull = true
    // 旋转屏幕方向
    scrcpy.main.startActivity(scrcpy.main.intent)
    scrcpy.main.requestedOrientation =
      if (remoteVideoWidth > remoteVideoHeight) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    val max = maxOf(scrcpy.main.appData.deviceWidth, scrcpy.main.appData.deviceHeight)
    val min = minOf(scrcpy.main.appData.deviceWidth, scrcpy.main.appData.deviceHeight)
    floatVideoParams.apply {
      x = 0
      y = 0
      width = if (remoteVideoWidth > remoteVideoHeight) max else min
      height = if (remoteVideoWidth > remoteVideoHeight) min else max
    }
    // 隐藏上下栏
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title1).visibility = View.GONE
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title2).visibility = View.GONE
    // 通知栏
    setNotification()
    // 监听导航悬浮球
    if (scrcpy.device.floatNav) setFloatNavListener()
    // 取消无用监听
    floatVideo.findViewById<LinearLayout>(R.id.float_video_bar).setOnTouchListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_stop).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_back).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener(null)
  }

  // 设置小窗
  private fun setSmallWindow() {
    // 竖屏打开
    if (remoteVideoHeight > remoteVideoWidth) {
      floatVideoParams.apply {
        x = scrcpy.main.appData.deviceWidth / 8
        y = scrcpy.main.appData.deviceHeight / 8
        height = scrcpy.main.appData.deviceHeight * 3 / 4
        width =
          (remoteVideoWidth.toFloat() / remoteVideoHeight.toFloat() * (height - scrcpy.main.resources.getDimension(
            R.dimen.floatVideoTitle
          ) * 0.7)).toInt()
      }
    }
    // 横屏打开
    else {
      floatVideoParams.apply {
        x = scrcpy.main.appData.deviceWidth / 8
        y = scrcpy.main.appData.deviceHeight / 8
        width = scrcpy.main.appData.deviceWidth * 3 / 4
        height =
          ((remoteVideoHeight.toFloat() / remoteVideoWidth.toFloat()) * width + scrcpy.main.resources.getDimension(
            R.dimen.floatVideoTitle
          ) * 0.7).toInt()
      }
    }
    setFloatBar()
    setStopListener()
    setNavListener()
    setSetSizeListener()
    setFloatVideoListener()
  }

  // 设置小小窗
  private fun setSmallSmall() {
    // 最小化小窗
    floatVideoParams.apply {
      if (remoteVideoWidth < remoteVideoHeight) {
        width = scrcpy.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
        height =
          (remoteVideoHeight.toFloat() / remoteVideoWidth.toFloat() * width + scrcpy.main.resources.getDimension(
            R.dimen.floatVideoTitle
          ) * 0.1).toInt()
      } else {
        height = scrcpy.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
        width =
          (remoteVideoWidth.toFloat() / remoteVideoHeight.toFloat() * (height - scrcpy.main.resources.getDimension(
            R.dimen.floatVideoTitle
          ) * 0.1)).toInt()
      }
    }
    update(true)
    // 取消无用监听
    floatVideo.setOnTouchListener(null)
    floatVideo.findViewById<LinearLayout>(R.id.float_video_bar).setOnTouchListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_stop).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_back).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener(null)
    floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).setOnTouchListener(null)
    // 取消焦点
    setFocus(false)
    // 设置监听
    val smallSmallGestureDetector =
      GestureDetector(scrcpy.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
          // 竖屏打开
          if (remoteVideoHeight > remoteVideoWidth) {
            floatVideoParams.apply {
              x = scrcpy.main.appData.deviceWidth / 8
              y = scrcpy.main.appData.deviceHeight / 8
              height = scrcpy.main.appData.deviceHeight * 3 / 4
              width =
                (remoteVideoWidth.toFloat() / remoteVideoHeight.toFloat() * (height - scrcpy.main.resources.getDimension(
                  R.dimen.floatVideoTitle
                ) * 0.7)).toInt()
            }
          }
          // 横屏打开
          else {
            floatVideoParams.apply {
              x = scrcpy.main.appData.deviceWidth / 8
              y = scrcpy.main.appData.deviceHeight / 8
              width = scrcpy.main.appData.deviceWidth * 3 / 4
              height =
                ((remoteVideoHeight.toFloat() / remoteVideoWidth.toFloat()) * width + scrcpy.main.resources.getDimension(
                  R.dimen.floatVideoTitle
                ) * 0.7).toInt()
            }
          }
          update(true)
          floatVideo.setOnTouchListener(null)
          setFloatBar()
          setStopListener()
          setNavListener()
          setSetSizeListener()
          setSurfaceListener()
          setFloatVideoListener()
          return super.onDoubleTap(event)
        }
      })
    // 记录按下坐标，避免设备过于敏感
    var xx = 0
    var yy = 0
    var isMoveVideo = false
    floatVideo.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.x.toInt()
          yy = event.y.toInt()
          smallSmallGestureDetector.onTouchEvent(event)
        }
        MotionEvent.ACTION_MOVE -> {
          val x = event.x.toInt()
          val y = event.y.toInt()
          if (!isMoveVideo) {
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) < 9) return@setOnTouchListener true
            isMoveVideo = true
            // 取消点击监控
            event.action = MotionEvent.ACTION_CANCEL
            smallSmallGestureDetector.onTouchEvent(event)
          }
          // 新位置
          val newX = event.rawX.toInt() - xx
          val newY = event.rawY.toInt() - yy
          // 移动悬浮窗
          floatVideoParams.x = newX
          floatVideoParams.y = newY
          update(false)
        }
        MotionEvent.ACTION_UP -> {
          isMoveVideo = false
          smallSmallGestureDetector.onTouchEvent(event)
        }
      }
      return@setOnTouchListener true
    }
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
    touchByteBuffer.putInt(x * remoteVideoWidth / localVideoWidth)
    touchByteBuffer.putInt(y * remoteVideoHeight / localVideoHeight)
    // 屏幕尺寸
    touchByteBuffer.putShort(remoteVideoWidth.toShort())
    touchByteBuffer.putShort(remoteVideoHeight.toShort())
    // 按压力度presureInt和buttons
    touchByteBuffer.putShort(1)
    touchByteBuffer.putInt(0)
    touchByteBuffer.putInt(0)
    touchByteBuffer.flip()
    controls.offer(touchByteBuffer.array())
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
    controls.offer(navByteBuffer.array())
  }

  // 检测旋转
  fun checkRotation(newWidth: Int, newHeight: Int) {
    if ((newWidth > newHeight) xor (localVideoWidth > localVideoHeight)) {
      val isLandscape = newWidth > newHeight
      val remoteVideoMax = maxOf(remoteVideoWidth, remoteVideoHeight)
      val remoteVideoMin = minOf(remoteVideoWidth, remoteVideoHeight)
      remoteVideoWidth = if (isLandscape) remoteVideoMax else remoteVideoMin
      remoteVideoHeight = if (isLandscape) remoteVideoMin else remoteVideoMax
      // 全屏or小窗
      if (scrcpy.device.isFull) {
        // 旋转屏幕方向
        scrcpy.main.requestedOrientation =
          if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // 导航球
        if (scrcpy.device.floatNav) floatNavParams.apply {
          x = 40
          y =
            (if (isLandscape) scrcpy.main.appData.deviceWidth else scrcpy.main.appData.deviceHeight) / 2
        }
        // 更新悬浮窗
        floatVideoParams.apply {
          width =
            if (isLandscape) scrcpy.main.appData.deviceHeight else scrcpy.main.appData.deviceWidth
          height =
            if (isLandscape) scrcpy.main.appData.deviceWidth else scrcpy.main.appData.deviceHeight
        }
      } else {
        // 更新悬浮窗
        floatVideoParams.apply {
          val metric = DisplayMetrics()
          scrcpy.main.windowManager.defaultDisplay.getRealMetrics(metric)
          val deviceWidth = metric.widthPixels
          val deviceHeight = metric.heightPixels
          // 防止旋转后超出界面
          x = if (x > deviceWidth) deviceWidth / 2 else x
          y = if (y > deviceHeight) deviceHeight / 2 else y
          width = localVideoHeight
          height =
            localVideoWidth + floatVideo.findViewById<LinearLayout>(R.id.float_video_title1).layoutParams.height * 2
          // 等比缩小，防止旋转后超出界面
          val xw = width - deviceWidth
          val xh = height - deviceHeight
          if (xw > 0 || xh > 0) {
            val tmp =
              if (xw > 0) xw.toFloat() / width.toFloat() else xh.toFloat() / height.toFloat()
            width = (width * (1 - tmp)).toInt()
            height = (height * (1 - tmp)).toInt()
          }
        }
      }
      update(true)
    }
  }

  // 设置焦点监听
  private fun setFloatVideoListener() {
    floatVideo.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_OUTSIDE -> {
          setFocus(false)
          return@setOnTouchListener true
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置视频区域触摸监听
  private fun setSurfaceListener() {
    // 视频触摸控制
    val pointerList = ArrayList<Int>(20)
    for (i in 1..20) pointerList.add(0)
    floatVideo.findViewById<SurfaceView>(R.id.float_video_surface).setOnTouchListener { _, event ->
      setFocus(true)
      try {
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            packTouchControl(MotionEvent.ACTION_DOWN, p, x, y)
            // 记录xy信息
            pointerList[p] = x
            pointerList[10 + p] = y
          }
          MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            packTouchControl(MotionEvent.ACTION_UP, p, x, y)
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
              packTouchControl(MotionEvent.ACTION_MOVE, p, x, y)
            }
          }
        }
      } catch (_: IllegalArgumentException) {
      }
      return@setOnTouchListener true
    }
  }

  // 设置三大金刚键监听控制
  private fun setNavListener() {
    floatVideo.findViewById<ImageView>(R.id.float_video_back).setOnClickListener {
      setFocus(true)
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener {
      setFocus(true)
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener {
      setFocus(true)
      packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_APP_SWITCH)
      packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_APP_SWITCH)
    }
  }

  // 设置悬浮窗大小拖动按钮监听控制
  private fun setSetSizeListener() {
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener { _, event ->
      setFocus(true)
      if (event.actionMasked == MotionEvent.ACTION_MOVE) {
        // 计算新大小（等比缩放，按照最长边缩放）
        val newWidth: Float
        val newHeight: Float
        if (floatVideoParams.width > floatVideoParams.height) {
          newHeight = event.rawY - floatVideoParams.y
          newWidth =
            newHeight * (floatVideoParams.width.toFloat() / floatVideoParams.height.toFloat())
        } else {
          newWidth = event.rawX - floatVideoParams.x
          newHeight =
            newWidth * (floatVideoParams.height.toFloat() / floatVideoParams.width.toFloat())
        }
        // 最小300个像素
        if (newWidth < 300 || newHeight < 300) return@setOnTouchListener true
        // 更新悬浮窗
        floatVideoParams.apply {
          width = newWidth.toInt()
          height = newHeight.toInt()
        }
        update(true)
      }
      return@setOnTouchListener true
    }
  }

  // 设置上横条监听控制
  private fun setFloatBar() {
    // 横条按钮监听
    val barGestureDetector =
      GestureDetector(scrcpy.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
          setFull()
          update(true)
          return super.onDoubleTap(event)
        }
      })
    // 记录按下坐标，避免设备过于敏感
    var xx = 0
    var yy = 0
    var isMoveVideoBar = false
    var statusBarHeight = 0
    // 获取状态栏高度
    val resourceId = scrcpy.main.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      statusBarHeight = scrcpy.main.resources.getDimensionPixelSize(resourceId)
    }
    floatVideo.findViewById<LinearLayout>(R.id.float_video_bar).setOnTouchListener { _, event ->
      setFocus(true)
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.x.toInt()
          yy = event.y.toInt()
          barGestureDetector.onTouchEvent(event)
        }
        MotionEvent.ACTION_MOVE -> {
          val x = event.x.toInt()
          val y = event.y.toInt()
          if (!isMoveVideoBar) {
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) < 9) return@setOnTouchListener true
            isMoveVideoBar = true
            // 取消点击监控
            event.action = MotionEvent.ACTION_CANCEL
            barGestureDetector.onTouchEvent(event)
          }
          // 贴边变成小小窗
          val criticality = scrcpy.main.resources.getDimension(R.dimen.floatNav).toInt()
          val metric = DisplayMetrics()
          scrcpy.main.windowManager.defaultDisplay.getRealMetrics(metric)
          val deviceWidth = metric.widthPixels
          val rawX = event.rawX.toInt()
          if ((rawX <= criticality || rawX >= (deviceWidth - criticality)) && event.rawY.toInt() <= criticality) {
            setSmallSmall()
            floatVideoParams.x =
              if (rawX <= criticality) scrcpy.main.resources.getDimension(R.dimen.floatVideoSmallSmallPadding)
                .toInt() else
                deviceWidth - floatVideoParams.width - scrcpy.main.resources.getDimension(R.dimen.floatVideoSmallSmallPadding)
                  .toInt()
            floatVideoParams.y =
              (scrcpy.main.resources.getDimension(R.dimen.floatVideoSmallSmallPadding) * 2).toInt()
            update(false)
          } else {
            // 新位置
            val newX = event.rawX.toInt() - xx - floatVideoParams.width / 4
            val newY = event.rawY.toInt() - yy
            // 避免移动至状态栏等不可触控区域
            if (newY < statusBarHeight + 10) return@setOnTouchListener true
            // 移动悬浮窗
            floatVideoParams.x = newX
            floatVideoParams.y = newY
            update(false)
          }
        }
        MotionEvent.ACTION_UP -> {
          isMoveVideoBar = false
          barGestureDetector.onTouchEvent(event)
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置关闭按钮监听控制
  private fun setStopListener() {
    floatVideo.findViewById<ImageView>(R.id.float_video_stop).setOnClickListener {
      scrcpy.stop()
    }
  }

  // 设置导航悬浮球监听控制
  private fun setFloatNavListener() {
    floatNav = LayoutInflater.from(scrcpy.main).inflate(R.layout.float_nav, null)
    // 导航球
    val gestureDetector =
      GestureDetector(scrcpy.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
          packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
          packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
          return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
          packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)
          packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME)
          return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
          packNavControl(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_APP_SWITCH)
          packNavControl(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_APP_SWITCH)
          super.onLongPress(e)
        }
      })
    // 手势处理
    var xx = 0
    var yy = 0
    // 导航悬浮球Layout
    floatNavParams = WindowManager.LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
      flags =
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      gravity = Gravity.START or Gravity.TOP
      width = scrcpy.main.resources.getDimension(R.dimen.floatNav).toInt()
      height = scrcpy.main.resources.getDimension(R.dimen.floatNav).toInt()
      x = 40
      y =
        (if (remoteVideoWidth > remoteVideoHeight) scrcpy.main.appData.deviceWidth else scrcpy.main.appData.deviceHeight) / 2
      format = PixelFormat.RGBA_8888
    }
    scrcpy.main.windowManager.addView(floatNav, floatNavParams)
    val width = scrcpy.main.resources.getDimension(R.dimen.floatNav).toInt() / 2
    floatNav.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.rawX.toInt()
          yy = event.rawY.toInt()
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
        MotionEvent.ACTION_MOVE -> {
          val x = event.rawX.toInt()
          val y = event.rawY.toInt()
          // 取消手势识别,适配一些机器将点击视作小范围移动(小于3的圆内不做处理)
          if (xx != -1) {
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) < 9) {
              return@setOnTouchListener true
            }
            xx = -1
            event.action = MotionEvent.ACTION_CANCEL
            gestureDetector.onTouchEvent(event)
          }
          floatNavParams.x = x - width
          floatNavParams.y = y - width
          scrcpy.main.windowManager.updateViewLayout(floatNav, floatNavParams)
          return@setOnTouchListener true
        }
        else -> {
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
      }
    }
  }

  // 获得焦点
  private var isFocus = true
  private fun setFocus(newFocus: Boolean) {
    if (newFocus != isFocus) {
      floatVideoParams.flags =
        if (newFocus) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        else WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      scrcpy.main.windowManager.updateViewLayout(
        floatVideo,
        floatVideoParams
      )
      isFocus = newFocus
    }
  }

  // 设置通知栏
  @SuppressLint("LaunchActivityFromNotification")
  private fun setNotification() {
    // 通知管理
    val notificationManager =
      scrcpy.main.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel("scrcpy_android", "chat", importance).apply {
        description = "常驻通知用于停止投屏"
      }
      notificationManager.createNotificationChannel(channel)
    }
    val intent = Intent("top.saymzx.scrcpy_android.notification")
    val builder =
      NotificationCompat.Builder(scrcpy.main, "scrcpy_android").setSmallIcon(R.drawable.icon)
        .setContentTitle(scrcpy.device.name).setContentText("点击关闭投屏")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(
          PendingIntent.getBroadcast(
            scrcpy.main,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
        ).setAutoCancel(true).setOngoing(true)
    notificationManager.notify(1, builder.build())
  }

}