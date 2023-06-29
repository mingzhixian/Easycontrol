package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.sqrt


@SuppressLint("ClickableViewAccessibility", "InternalInsetResource", "DiscouragedApi")
class FloatVideo(
  private val device: Device,
  var remoteVideoWidth: Int,
  var remoteVideoHeight: Int,
  val touchHandle: (byteArray: ByteArray) -> Unit
) {
  // 悬浮窗
  lateinit var floatVideo: View

  // 悬浮窗Layout
  private var floatVideoParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
    type =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else WindowManager.LayoutParams.TYPE_PHONE
    flags =
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    gravity = Gravity.START or Gravity.TOP
    format = PixelFormat.RGBA_8888
  }

  // 导航悬浮球
  @SuppressLint("InflateParams")
  lateinit var floatNav: View
  private lateinit var floatNavParams: WindowManager.LayoutParams

  // 视频大小
  private var localVideoWidth = 0
  private var localVideoHeight = 0

  // 显示悬浮窗
  @SuppressLint("InflateParams")
  fun show() {
    appData.main.runOnUiThread {
      floatVideo = appData.main.layoutInflater.inflate(R.layout.float_video, null, false)
      // 设置视频界面触摸监听
      setSurfaceListener()
      // 全屏or小窗模式
      appData.main.windowManager.addView(floatVideo, floatVideoParams)
      if (device.isFull) setFull() else setSmallWindow()
      update(true)
    }
  }

  // 隐藏悬浮窗
  fun hide() {
    try {
      hideFloatNav()
      appData.main.windowManager.removeView(floatVideo)
    } catch (_: Exception) {
    }
  }

  // 隐藏导航球
  private fun hideFloatNav() {
    try {
      appData.main.windowManager.removeView(
        floatNav
      )
    } catch (_: Exception) {
    }
  }

  // 退出
  private fun exit() {
    for (i in appData.devices) {
      if (i.name == device.name) {
        i.scrcpy?.stop("用户停止", null)
        break
      }
    }
  }

  // 更新悬浮窗
  private fun update(hasChangeSize: Boolean) {
    appData.main.windowManager.updateViewLayout(
      floatVideo, floatVideoParams
    )
    if (device.isFull) appData.main.windowManager.updateViewLayout(floatNav, floatNavParams)
    // 减少未修改大小的无用调用
    if (hasChangeSize && !device.isFull) {
      // 等比缩放控件大小
      val tmp =
        sqrt((localVideoWidth * localVideoHeight).toDouble() / (appData.deviceWidth * appData.deviceHeight).toDouble())
      // 整体背景圆角
      val floatVideoShape = floatVideo.background as GradientDrawable
      floatVideoShape.cornerRadius =
        appData.main.resources.getDimension(R.dimen.floatVideoBackground) * tmp.toFloat()
      // 上空白
      val floatVideoTitle1 = floatVideo.findViewById<LinearLayout>(R.id.float_video_title1)
      val floatVideoTitle1LayoutParams = floatVideoTitle1.layoutParams
      floatVideoTitle1LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoTitle1.layoutParams = floatVideoTitle1LayoutParams
      // 下空白
      val floatVideoTitle2 = floatVideo.findViewById<LinearLayout>(R.id.float_video_title2)
      val floatVideoTitle2LayoutParams = floatVideoTitle1.layoutParams
      floatVideoTitle2LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoTitle2.layoutParams = floatVideoTitle2LayoutParams
      // 横条
      val floatVideoBar = floatVideo.findViewById<View>(R.id.float_video_bar)
      val floatVideoBarLayoutParams = floatVideoBar.layoutParams
      floatVideoBarLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoBar.layoutParams = floatVideoBarLayoutParams
      floatVideoBar.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoBarPadding) * tmp).toInt())
      // 停止按钮
      val floatVideoStop = floatVideo.findViewById<ImageView>(R.id.float_video_stop)
      val floatVideoStopLayoutParams = floatVideoStop.layoutParams
      floatVideoStopLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoStop.layoutParams = floatVideoStopLayoutParams
      floatVideoStop.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
      // 最近任务键
      val floatVideoSwitch = floatVideo.findViewById<ImageView>(R.id.float_video_switch)
      val floatVideoSwitchLayoutParams = floatVideoSwitch.layoutParams
      floatVideoSwitchLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoSwitch.layoutParams = floatVideoSwitchLayoutParams
      floatVideoSwitch.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 桌面键
      val floatVideoHome = floatVideo.findViewById<ImageView>(R.id.float_video_home)
      val floatVideoHomeLayoutParams = floatVideoHome.layoutParams
      floatVideoHomeLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoHome.layoutParams = floatVideoHomeLayoutParams
      floatVideoHome.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 返回键
      val floatVideoBack = floatVideo.findViewById<ImageView>(R.id.float_video_back)
      val floatVideoBackLayoutParams = floatVideoBack.layoutParams
      floatVideoBackLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoBack.layoutParams = floatVideoBackLayoutParams
      floatVideoBack.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 修改大小按钮
      val floatVideoSetSize = floatVideo.findViewById<ImageView>(R.id.float_video_set_size)
      val floatVideoSetSizeLayoutParams = floatVideoSetSize.layoutParams
      floatVideoSetSizeLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideoSetSize.layoutParams = floatVideoSetSizeLayoutParams
      floatVideoSetSize.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
    }
  }

  // 设置全屏
  private fun setFull() {
    // 取消焦点
    setFocus(false)
    device.isFull = true
    // 旋转屏幕方向
    val isLandScape = remoteVideoWidth > remoteVideoHeight
    // 进入专注模式
    goToFocus(isLandScape)
    floatVideoParams.apply {
      x = 0
      y = 0
      width = if (isLandScape) appData.deviceHeight else appData.deviceWidth
      height = if (isLandScape) appData.deviceWidth else appData.deviceHeight
    }
    localVideoWidth = floatVideoParams.width
    localVideoHeight = floatVideoParams.height
    // 隐藏上下栏
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title1).visibility = View.GONE
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title2).visibility = View.GONE
    // 监听导航悬浮球
    setFloatNavListener()
    // 取消无用监听
    floatVideo.setOnTouchListener(null)
    floatVideo.findViewById<LinearLayout>(R.id.float_video_bar).setOnTouchListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_stop).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_back).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener(null)
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener(null)
  }

  // 设置小窗
  private fun setSmallWindow() {
    device.isFull = false
    // 显示上下栏
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title1).visibility = View.VISIBLE
    floatVideo.findViewById<LinearLayout>(R.id.float_video_title2).visibility = View.VISIBLE
    // 隐藏导航球
    try {
      floatNav.setOnTouchListener(null)
    } catch (_: Exception) {
    }
    hideFloatNav()
    // 竖屏打开
    if (remoteVideoHeight > remoteVideoWidth) {
      floatVideoParams.apply {
        val tmpHeight = appData.deviceHeight * 3 / 4
        val tmpWidth = tmpHeight * remoteVideoWidth / remoteVideoHeight
        calculateFloatSize(tmpWidth, tmpHeight)
        x = (appData.deviceWidth - width) / 2
        y = (appData.deviceHeight - height) / 2
      }
    }
    // 横屏打开
    else {
      floatVideoParams.apply {
        val tmpWidth = appData.deviceWidth * 3 / 4
        val tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
        calculateFloatSize(tmpWidth, tmpHeight)
        x = (appData.deviceWidth - width) / 2
        y = (appData.deviceHeight - height) / 2
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
        val tmpWidth = appData.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
        val tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
        calculateFloatSize(tmpWidth, tmpHeight)
      } else {
        val tmpHeight = appData.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
        val tmpWidth = tmpHeight * remoteVideoWidth / remoteVideoHeight
        calculateFloatSize(tmpWidth, tmpHeight)
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
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
          setSmallWindow()
          update(true)
          floatVideo.setOnTouchListener(null)
          setSurfaceListener()
          setFocus(true)
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
    touchHandle(touchByteBuffer.array())
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
    touchHandle(navByteBuffer.array())
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
      if (device.isFull) {
        // 旋转屏幕方向
        goToFocus(isLandscape)
        // 导航球
        floatNavParams.apply {
          x = 40
          y = (if (isLandscape) appData.deviceWidth else appData.deviceHeight) / 2
        }
        // 更新悬浮窗
        floatVideoParams.apply {
          width = if (isLandscape) appData.deviceHeight else appData.deviceWidth
          height = if (isLandscape) appData.deviceWidth else appData.deviceHeight
        }
        localVideoWidth = floatVideoParams.width
        localVideoHeight = floatVideoParams.height
      } else {
        // 更新悬浮窗
        floatVideoParams.apply {
          // 防止旋转后超出界面
          val tmpWidth: Int
          val tmpHeight: Int
          // 竖屏
          if (remoteVideoHeight > remoteVideoWidth) {
            tmpHeight = appData.deviceHeight * 3 / 4
            tmpWidth = tmpHeight * remoteVideoWidth / remoteVideoHeight
          }
          // 横屏
          else {
            tmpWidth = appData.deviceWidth * 3 / 4
            tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
          }
          calculateFloatSize(tmpWidth, tmpHeight)
          x = (appData.deviceWidth - width) / 2
          y = (appData.deviceHeight - height) / 2
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
      sendNavKey(4)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_home).setOnClickListener {
      setFocus(true)
      sendNavKey(3)
    }
    floatVideo.findViewById<ImageView>(R.id.float_video_switch).setOnClickListener {
      setFocus(true)
      sendNavKey(187)
    }
  }

  // 设置悬浮窗大小拖动按钮监听控制
  private fun setSetSizeListener() {
    floatVideo.findViewById<ImageView>(R.id.float_video_set_size).setOnTouchListener { _, event ->
      setFocus(true)
      if (event.actionMasked == MotionEvent.ACTION_MOVE) {
        // 计算新大小（等比缩放）
        val tmpWidth = event.rawX - floatVideoParams.x
        val tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
        // 最小300个像素
        if (tmpWidth < 300 || tmpHeight < 300) return@setOnTouchListener true
        calculateFloatSize(tmpWidth.toInt(), tmpHeight.toInt())
        update(true)
      }
      return@setOnTouchListener true
    }
  }

  // 设置上横条监听控制
  private fun setFloatBar() {
    // 横条按钮监听
    val barGestureDetector =
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
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
    val resourceId = appData.main.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      statusBarHeight = appData.main.resources.getDimensionPixelSize(resourceId)
    }
    var deviceWidth = 0
    floatVideo.findViewById<LinearLayout>(R.id.float_video_bar).setOnTouchListener { _, event ->
      setFocus(true)
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.x.toInt()
          yy = event.y.toInt()
          barGestureDetector.onTouchEvent(event)
          val metric = DisplayMetrics()
          appData.main.windowManager.defaultDisplay.getRealMetrics(metric)
          deviceWidth = metric.widthPixels
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
          val criticality = appData.main.resources.getDimension(R.dimen.floatNav).toInt()
          val rawX = event.rawX.toInt()
          if ((rawX <= criticality || rawX >= (deviceWidth - criticality)) && event.rawY.toInt() <= criticality) {
            setSmallSmall()
            floatVideoParams.x =
              if (rawX <= criticality) appData.main.resources.getDimension(R.dimen.floatVideoSmallSmallPadding)
                .toInt() else deviceWidth - floatVideoParams.width - appData.main.resources.getDimension(
                R.dimen.floatVideoSmallSmallPadding
              ).toInt()
            floatVideoParams.y =
              (appData.main.resources.getDimension(R.dimen.floatVideoSmallSmallPadding) * 2).toInt()
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
      exit()
    }
  }

  // 设置导航悬浮球监听控制
  @SuppressLint("InflateParams")
  private fun setFloatNavListener() {
    floatNav = appData.main.layoutInflater.inflate(R.layout.float_nav, null)
    // 导航球
    val gestureDetector =
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
          sendNavKey(4)
          return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
          sendNavKey(3)
          return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
          setFloatNavMenuListener()
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
      width = appData.main.resources.getDimension(R.dimen.floatNav).toInt()
      height = appData.main.resources.getDimension(R.dimen.floatNav).toInt()
      x = 40
      y =
        (if (remoteVideoWidth > remoteVideoHeight) appData.deviceWidth else appData.deviceHeight) / 2
      format = PixelFormat.RGBA_8888
    }
    appData.main.windowManager.addView(floatNav, floatNavParams)
    val width = appData.main.resources.getDimension(R.dimen.floatNav).toInt() / 2
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
          appData.main.windowManager.updateViewLayout(floatNav, floatNavParams)
          return@setOnTouchListener true
        }

        else -> {
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
      }
    }
  }

  // 设置导航球菜单监听
  private fun setFloatNavMenuListener() {
    // 展示MENU
    floatNavParams.width = appData.main.resources.getDimension(R.dimen.floatNavMenuW).toInt()
    floatNavParams.height = appData.main.resources.getDimension(R.dimen.floatNavMenuH).toInt()
    appData.main.windowManager.updateViewLayout(floatNav, floatNavParams)
    floatNav.findViewById<LinearLayout>(R.id.float_nav_menu).visibility = View.VISIBLE
    floatNav.findViewById<ImageView>(R.id.float_nav_image).visibility = View.GONE
    // 返回导航球
    floatNav.findViewById<TextView>(R.id.float_nav_back).setOnClickListener {
      backFloatNav()
    }
    // 发送最近任务键
    floatNav.findViewById<TextView>(R.id.float_nav_switch).setOnClickListener {
      sendNavKey(187)
      backFloatNav()
    }
    // 退出全屏
    floatNav.findViewById<TextView>(R.id.float_nav_exit_full).setOnClickListener {
      setSmallWindow()
      update(true)
    }
    // 退出
    floatNav.findViewById<TextView>(R.id.float_nav_exit).setOnClickListener {
      exit()
    }
  }

  // 回到导航球模式
  private fun backFloatNav() {
    floatNavParams.width = appData.main.resources.getDimension(R.dimen.floatNav).toInt()
    floatNavParams.height = appData.main.resources.getDimension(R.dimen.floatNav).toInt()
    appData.main.windowManager.updateViewLayout(floatNav, floatNavParams)
    floatNav.findViewById<ImageView>(R.id.float_nav_image).visibility = View.VISIBLE
    floatNav.findViewById<LinearLayout>(R.id.float_nav_menu).visibility = View.GONE
  }

  // 发送导航按键
  private fun sendNavKey(key: Int) {
    packNavControl(0, key)
    packNavControl(1, key)
  }

  // 获得焦点
  private var isFocus = false
  private fun setFocus(newFocus: Boolean) {
    if (!device.isFull && newFocus != isFocus) {
      floatVideoParams.flags =
        if (newFocus) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        else WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      appData.main.windowManager.updateViewLayout(
        floatVideo, floatVideoParams
      )
      isFocus = newFocus
    }
  }

  // 计算悬浮窗大小
  private fun calculateFloatSize(tmpWidth: Int, tmpHeight: Int) {
    localVideoWidth = tmpWidth
    localVideoHeight = tmpHeight
    val tmp =
      sqrt((tmpHeight * tmpWidth).toDouble() / (appData.deviceWidth * appData.deviceHeight).toDouble())
    floatVideoParams.apply {
      width = tmpWidth
      height = (tmpHeight + 2 * tmp * appData.main.resources.getDimension(
        R.dimen.floatVideoTitle
      )).toInt()
    }
  }

  // 进入专注模式
  private fun goToFocus(isLandScape:Boolean){
    appData.focusIsLandScape=isLandScape
    val intent=Intent(appData.main, FullScreenActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    appData.main.startActivity(intent)
  }

}