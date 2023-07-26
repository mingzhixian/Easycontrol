package top.saymzx.scrcpy.android.entity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import androidx.core.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.scrcpy.android.FullScreenActivity
import top.saymzx.scrcpy.android.R
import top.saymzx.scrcpy.android.appData
import top.saymzx.scrcpy.android.databinding.FloatNavBinding
import top.saymzx.scrcpy.android.databinding.FloatVideoBinding
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility", "InternalInsetResource", "DiscouragedApi")
class FloatVideo(
  private val device: Device,
  var remoteVideoWidth: Int,
  var remoteVideoHeight: Int,
  val touchHandle: (byteArray: ByteArray) -> Unit
) {
  // 悬浮窗
  lateinit var floatVideo: FloatVideoBinding

  // 悬浮设置
  private val baseLayoutParamsFlag =
    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
  private val floatNavLayoutParamsFlag =
    baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
  private val floatVideoLayoutParamsFlagNoFocus =
    baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
  private val floatVideoLayoutParamsFlagFocus =
    baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

  // 悬浮窗Layout
  private var floatVideoParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
    type =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else WindowManager.LayoutParams.TYPE_PHONE
    flags = floatVideoLayoutParamsFlagFocus
    gravity = Gravity.START or Gravity.TOP
    format = PixelFormat.TRANSLUCENT
  }

  // 导航悬浮球
  @SuppressLint("InflateParams")
  lateinit var floatNav: FloatNavBinding
  private lateinit var floatNavParams: WindowManager.LayoutParams

  // 视频大小
  private var localVideoWidth = 0
  private var localVideoHeight = 0

  // 显示悬浮窗
  @SuppressLint("InflateParams")
  fun show() {
    appData.main.runOnUiThread {
      floatVideo = FloatVideoBinding.inflate(appData.main.layoutInflater)
      // 设置视频界面触摸监听
      setSurfaceListener()
      // 全屏or小窗模式
      appData.main.windowManager.addView(floatVideo.root, floatVideoParams)
      if (device.isFull) setFull() else setSmallWindow()
      update(true)
    }
  }

  // 隐藏悬浮窗
  fun hide() {
    try {
      hideFloatNav()
      appData.main.windowManager.removeView(floatVideo.root)
    } catch (_: Exception) {
    }
  }

  // 隐藏导航球
  private fun hideFloatNav() {
    try {
      appData.main.windowManager.removeView(
        floatNav.root
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
      floatVideo.root, floatVideoParams
    )
    if (device.isFull) appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    // 减少未修改大小的无用调用
    if (hasChangeSize && !device.isFull) {
      // 等比缩放控件大小
      val tmp =
        sqrt((localVideoWidth * localVideoHeight).toDouble() / (appData.deviceWidth * appData.deviceHeight).toDouble())
      // 整体背景圆角
      val floatVideoShape = floatVideo.floatVideo.background as GradientDrawable
      floatVideoShape.cornerRadius =
        appData.main.resources.getDimension(R.dimen.floatVideoBackground) * tmp.toFloat()
      // 上空白
      val floatVideoTitle1LayoutParams = floatVideo.floatVideoTitle1.layoutParams
      floatVideoTitle1LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoTitle1.layoutParams = floatVideoTitle1LayoutParams
      // 下空白
      val floatVideoTitle2LayoutParams = floatVideo.floatVideoTitle2.layoutParams
      floatVideoTitle2LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoTitle2.layoutParams = floatVideoTitle2LayoutParams
      // 全屏按钮
      val floatVideoFullscreenLayoutParams = floatVideo.floatVideoFullscreen.layoutParams
      floatVideoFullscreenLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoFullscreen.layoutParams = floatVideoFullscreenLayoutParams
      floatVideo.floatVideoFullscreen.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 横条
      val floatVideoBarLayoutParams = floatVideo.floatVideoBar.layoutParams
      floatVideoBarLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoBar.layoutParams = floatVideoBarLayoutParams
      floatVideo.floatVideoBar.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoBarPadding) * tmp).toInt())
      // 停止按钮
      val floatVideoStopLayoutParams = floatVideo.floatVideoStop.layoutParams
      floatVideoStopLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoStop.layoutParams = floatVideoStopLayoutParams
      floatVideo.floatVideoStop.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
      // 刷新率
      floatVideo.floatVideoFps.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt())
      // 最近任务键
      val floatVideoSwitchLayoutParams = floatVideo.floatVideoSwitch.layoutParams
      floatVideoSwitchLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoSwitch.layoutParams = floatVideoSwitchLayoutParams
      floatVideo.floatVideoSwitch.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 桌面键
      val floatVideoHomeLayoutParams = floatVideo.floatVideoHome.layoutParams
      floatVideoHomeLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoHome.layoutParams = floatVideoHomeLayoutParams
      floatVideo.floatVideoHome.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 返回键
      val floatVideoBackLayoutParams = floatVideo.floatVideoBack.layoutParams
      floatVideoBackLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoBack.layoutParams = floatVideoBackLayoutParams
      floatVideo.floatVideoBack.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoNavButtonPadding) * tmp).toInt())
      // 修改大小按钮
      val floatVideoSetSizeLayoutParams = floatVideo.floatVideoSetSize.layoutParams
      floatVideoSetSizeLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoSetSize.layoutParams = floatVideoSetSizeLayoutParams
      floatVideo.floatVideoSetSize.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
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
    appData.isFocus = true
    appData.fullScreenOrientation =
      if (isLandScape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    appData.main.startActivity(Intent(appData.main, FullScreenActivity::class.java))
    localVideoWidth = if (isLandScape) appData.deviceHeight else appData.deviceWidth
    localVideoHeight = if (isLandScape) appData.deviceWidth else appData.deviceHeight
    // 更新悬浮窗
    floatVideoParams.apply {
      x = -(add2DpPx / 2)
      y = -(add2DpPx / 2)
      width = localVideoWidth + add2DpPx
      height = localVideoHeight + add2DpPx
    }
    // 隐藏上下栏
    floatVideo.floatVideoTitle1.visibility = View.GONE
    floatVideo.floatVideoTitle2.visibility = View.GONE
    // 监听导航悬浮球
    setFloatNavListener()
    // 取消无用监听
    floatVideo.root.setOnTouchListener(null)
    floatVideo.floatVideoFullscreen.setOnClickListener(null)
    floatVideo.floatVideoBar.setOnTouchListener(null)
    floatVideo.floatVideoStop.setOnClickListener(null)
    floatVideo.floatVideoBack.setOnClickListener(null)
    floatVideo.floatVideoHome.setOnClickListener(null)
    floatVideo.floatVideoSwitch.setOnClickListener(null)
    floatVideo.floatVideoSetSize.setOnTouchListener(null)
  }

  // 设置小窗
  private fun setSmallWindow() {
    device.isFull = false
    appData.isFocus = false
    // 显示上下栏
    floatVideo.floatVideoTitle1.visibility = View.VISIBLE
    floatVideo.floatVideoTitle2.visibility = View.VISIBLE
    // 隐藏导航球
    try {
      floatNav.root.setOnTouchListener(null)
    } catch (_: Exception) {
    }
    hideFloatNav()
    // 计算位置和大小
    calculateFloatSite(remoteVideoWidth, remoteVideoHeight)
    setFullScreen()
    setFloatBar()
    setStopListener()
    setNavListener()
    setSetSizeListener()
    setFloatVideoListener()
  }

  // 设置小小窗
  private fun setSmallSmall() {
    val oldSize = Pair(localVideoWidth, localVideoHeight)
    if (remoteVideoWidth < remoteVideoHeight) {
      val tmpWidth = appData.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
      val tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
      calculateFloatSize(tmpWidth, tmpHeight)
    } else {
      val tmpHeight = appData.main.resources.getDimension(R.dimen.floatVideoSmallSmall).toInt()
      val tmpWidth = tmpHeight * remoteVideoWidth / remoteVideoHeight
      calculateFloatSize(tmpWidth, tmpHeight)
    }
    update(true)
    // 取消无用监听
    floatVideo.root.setOnTouchListener(null)
    floatVideo.floatVideoFullscreen.setOnClickListener(null)
    floatVideo.floatVideoBar.setOnTouchListener(null)
    floatVideo.floatVideoStop.setOnClickListener(null)
    floatVideo.floatVideoBack.setOnClickListener(null)
    floatVideo.floatVideoHome.setOnClickListener(null)
    floatVideo.floatVideoSwitch.setOnClickListener(null)
    floatVideo.floatVideoSetSize.setOnTouchListener(null)
    floatVideo.floatVideoSurface.setOnTouchListener(null)
    // 取消焦点
    setFocus(false)
    // 设置监听
    floatVideo.root.setOnClickListener {
      setSmallWindow()
      calculateFloatSize(oldSize.first, oldSize.second)
      floatVideoParams.apply {
        x = oldSite.first
        y = oldSite.second
      }
      update(true)
      floatVideo.root.setOnTouchListener(null)
      setSurfaceListener()
      setFocus(true)
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
  private val add2DpPx = appData.publicTools.dp2px(2f).toInt()
  fun checkRotation(newWidth: Int, newHeight: Int) {
    if ((newWidth > newHeight) xor (localVideoWidth > localVideoHeight)) {
      val isLandScape = newWidth > newHeight
      val remoteVideoMax = maxOf(remoteVideoWidth, remoteVideoHeight)
      val remoteVideoMin = minOf(remoteVideoWidth, remoteVideoHeight)
      remoteVideoWidth = if (isLandScape) remoteVideoMax else remoteVideoMin
      remoteVideoHeight = if (isLandScape) remoteVideoMin else remoteVideoMax
      // 全屏or小窗
      if (device.isFull) {
        // 旋转屏幕方向
        appData.fullScreenOrientation =
          if (isLandScape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // 导航球
        floatNavParams.apply {
          x = 40
          y = (if (isLandScape) appData.deviceWidth else appData.deviceHeight) / 2
        }
        localVideoWidth = if (isLandScape) appData.deviceHeight else appData.deviceWidth
        localVideoHeight = if (isLandScape) appData.deviceWidth else appData.deviceHeight
        // 更新悬浮窗
        floatVideoParams.apply {
          width = localVideoWidth + add2DpPx
          height = localVideoHeight + add2DpPx
        }
      } else {
        // 计算位置和大小
        calculateFloatSite(remoteVideoWidth, remoteVideoHeight)
      }
      update(true)
    }
  }

  // 设置焦点监听
  private fun setFloatVideoListener() {
    floatVideo.root.setOnTouchListener { _, event ->
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
    floatVideo.floatVideoSurface.setOnTouchListener { _, event ->
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
    floatVideo.floatVideoBack.setOnClickListener {
      setFocus(true)
      sendNavKey(4)
    }
    floatVideo.floatVideoHome.setOnClickListener {
      setFocus(true)
      sendNavKey(3)
    }
    floatVideo.floatVideoSwitch.setOnClickListener {
      setFocus(true)
      sendNavKey(187)
    }
  }

  // 设置悬浮窗大小拖动按钮监听控制
  private fun setSetSizeListener() {
    var width = 0f
    val maxCal = appData.publicTools.dp2px(30f)
    floatVideo.floatVideoSetSize.setOnTouchListener { _, event ->
      setFocus(true)
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          width = event.rawX - floatVideoParams.x
        }

        MotionEvent.ACTION_MOVE -> {
          // 计算新大小（等比缩放）
          val tmpWidth = event.rawX - floatVideoParams.x
          val tmpHeight = tmpWidth * remoteVideoHeight / remoteVideoWidth
          // 最小300个像素
          if (tmpWidth < 300 || tmpHeight < 300) return@setOnTouchListener true
          calculateFloatSize(tmpWidth.toInt(), tmpHeight.toInt())
          if (abs(tmpWidth - width) > maxCal) {
            width = tmpWidth
            update(true)
          } else update(false)
        }

        MotionEvent.ACTION_UP -> {
          update(true)
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置全屏按钮监听
  private fun setFullScreen() {
    floatVideo.floatVideoFullscreen.setOnClickListener {
      setFull()
      update(true)
    }
  }

  // 设置上横条监听控制
  private lateinit var oldSite: Pair<Int, Int>
  private fun setFloatBar() {
    var statusBarHeight = 0
    // 获取状态栏高度
    val resourceId = appData.main.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      statusBarHeight = appData.main.resources.getDimensionPixelSize(resourceId)
    }
    var deviceWidth = 0
    val criticality = appData.publicTools.dp2px(60f).toInt()
    var x = 0
    var y = 0
    floatVideo.floatVideoBar.setOnTouchListener { _, event ->
      setFocus(true)
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          x = event.x.toInt()
          y = event.y.toInt()
          oldSite = Pair(floatVideoParams.x, floatVideoParams.y)
          val metric = DisplayMetrics()
          appData.main.windowManager.defaultDisplay.getRealMetrics(metric)
          deviceWidth = metric.widthPixels
        }

        MotionEvent.ACTION_MOVE -> {
          // 贴边变成小小窗
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
            val newX = event.rawX.toInt() - x - floatVideoParams.width / 4
            val newY = event.rawY.toInt() - y
            // 避免移动至状态栏等不可触控区域
            if (newY < statusBarHeight + 10) return@setOnTouchListener true
            // 移动悬浮窗
            floatVideoParams.x = newX
            floatVideoParams.y = newY
            update(false)
          }
        }

        MotionEvent.ACTION_UP -> {
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置关闭按钮监听控制
  private fun setStopListener() {
    floatVideo.floatVideoStop.setOnClickListener {
      exit()
    }
  }

  // 设置导航悬浮球监听控制
  @SuppressLint("InflateParams")
  private fun setFloatNavListener() {
    floatNav = FloatNavBinding.inflate(appData.main.layoutInflater)
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
    val floatNavSize = appData.publicTools.dp2px(appData.setValue.floatNavSize.toFloat()).toInt()
    // 导航悬浮球Layout
    floatNavParams = WindowManager.LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
      flags = floatNavLayoutParamsFlag
      gravity = Gravity.START or Gravity.TOP
      width = floatNavSize
      height = floatNavSize
      x = 40
      y =
        (if (remoteVideoWidth > remoteVideoHeight) appData.deviceWidth else appData.deviceHeight) / 2
      format = PixelFormat.TRANSLUCENT
    }
    appData.main.windowManager.addView(floatNav.root, floatNavParams)
    val width = floatNavSize / 2
    floatNav.root.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          floatNav.floatNavImage.alpha = 1f
          xx = event.rawX.toInt()
          yy = event.rawY.toInt()
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }

        MotionEvent.ACTION_MOVE -> {
          val x = event.rawX.toInt()
          val y = event.rawY.toInt()
          // 取消手势识别,适配一些机器将点击视作小范围移动(小于4的圆内不做处理)
          if (xx != -1) {
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) < 16) {
              return@setOnTouchListener true
            }
            xx = -1
            event.action = MotionEvent.ACTION_CANCEL
            gestureDetector.onTouchEvent(event)
          }
          floatNavParams.x = x - width
          floatNavParams.y = y - width
          appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
          return@setOnTouchListener true
        }

        else -> {
          gestureDetector.onTouchEvent(event)
          appData.mainScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
              floatNav.floatNavImage.alpha = 0.7f
            }
          }
          return@setOnTouchListener true
        }
      }
    }
  }

  // 设置导航球菜单监听
  private var floatNavSite = Pair(false, 0)
  private fun setFloatNavMenuListener() {
    // 展示MENU
    val menuWidth = appData.main.resources.getDimension(R.dimen.floatNavMenuW).toInt()
    floatNavParams.width = menuWidth
    floatNavParams.height = appData.main.resources.getDimension(R.dimen.floatNavMenuH).toInt()
    appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    floatNav.floatNavMenu.visibility = View.VISIBLE
    floatNav.floatNavImage.visibility = View.GONE
    // 获取系统宽高
    val size = appData.publicTools.getScreenSize(appData.main)
    if (floatNavParams.x + menuWidth > size.first) {
      floatNavSite = Pair(true, floatNavParams.x)
      floatNavParams.x = size.first - menuWidth
      appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    }
    // 返回导航球
    floatNav.floatNavBack.setOnClickListener {
      backFloatNav()
    }
    // 发送最近任务键
    floatNav.floatNavSwitch.setOnClickListener {
      sendNavKey(187)
      backFloatNav()
    }
    // 退出全屏
    floatNav.floatNavExitFull.setOnClickListener {
      setSmallWindow()
      update(true)
    }
    // 退出
    floatNav.floatNavExit.setOnClickListener {
      exit()
    }
  }

  // 回到导航球模式
  private fun backFloatNav() {
    val floatNavSize = appData.publicTools.dp2px(appData.setValue.floatNavSize.toFloat()).toInt()
    floatNavParams.width = floatNavSize
    floatNavParams.height = floatNavSize
    if (floatNavSite.first) {
      floatNavParams.x = floatNavSite.second
      floatNavSite = Pair(false, 0)
    }
    appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    floatNav.floatNavImage.visibility = View.VISIBLE
    floatNav.floatNavMenu.visibility = View.GONE
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
        if (newFocus) floatVideoLayoutParamsFlagFocus else floatVideoLayoutParamsFlagNoFocus
      appData.main.windowManager.updateViewLayout(
        floatVideo.root, floatVideoParams
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

  // 计算悬浮窗位置
  private fun calculateFloatSite(tmpWidth: Int, tmpHeight: Int) {
    // 获取当前屏幕大小
    val screenSize = appData.publicTools.getScreenSize(appData.main)
    // 横向最大不会超出
    if (screenSize.second > (screenSize.first * tmpHeight / tmpWidth.toFloat())) {
      val tmp2Width = screenSize.first * 3 / 4
      val tmp2Height = tmp2Width * remoteVideoHeight / remoteVideoWidth
      calculateFloatSize(tmp2Width, tmp2Height)
    }
    // 竖向最大不会超出
    else {
      val tmp2Height = screenSize.second * 3 / 4
      val tmp2Width = tmp2Height * remoteVideoWidth / remoteVideoHeight
      calculateFloatSize(tmp2Width, tmp2Height)
    }
    // 居中显示
    floatVideoParams.apply {
      x = (screenSize.first - width) / 2
      y = (screenSize.second - height) / 2
    }
  }

}