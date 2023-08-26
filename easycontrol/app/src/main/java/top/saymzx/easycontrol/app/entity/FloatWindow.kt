package top.saymzx.easycontrol.app.entity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.*
import android.view.ViewGroup.LayoutParams
import androidx.core.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.saymzx.easycontrol.app.MasterActivity
import top.saymzx.easycontrol.app.R
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.client.Client
import top.saymzx.easycontrol.app.client.HandleUi
import top.saymzx.easycontrol.app.databinding.ModuleFloatWindowBinding
import java.util.*
import kotlin.math.sqrt


@SuppressLint("ClickableViewAccessibility", "InternalInsetResource", "DiscouragedApi")
class FloatWindow(private val device: Device) : HandleUi {

  // Client
  private var client: Client? = null

  // 悬浮球
  private var floatBall: FloatBall? = null

  // 悬浮窗
  private var hasShow = false
  private val floatWindow = ModuleFloatWindowBinding.inflate(appData.main.layoutInflater)
  private val floatWindowParams = WindowManager.LayoutParams().apply {
    type = floatType
    flags = floatVideoLayoutParamsFlagFocus
    gravity = Gravity.START or Gravity.TOP
    format = PixelFormat.TRANSLUCENT
    x = 200
    y = 200
    width = 300
    height = 300
  }

  // 小窗大小
  private lateinit var surfaceSize: Pair<Int, Int>

  // 显示悬浮窗
  init {
    // 创建悬浮窗
    hasShow = true
    appData.main.windowManager.addView(floatWindow.root, floatWindowParams)
    floatWindow.floatWindowMiniText.text = device.name
    // 关闭按钮
    floatWindow.floatWindowStop.setOnClickListener {
      hide()
    }
    floatWindow.floatWindowSurface.doOnPreDraw {
      client = Client(device, floatWindow.floatWindowSurface.holder.surface, this)
      (client as Thread).start()
    }
  }

  // 隐藏悬浮窗
  private fun hide() {
    if (hasShow) {
      hasShow = false
      MasterActivity.isFocus = false
      appData.main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      floatBall?.hide()
      appData.main.windowManager.removeView(floatWindow.root)
      client?.stop("停止", null)
    }
  }

  // 更新悬浮窗
  private val tmpDeviceSize = appData.publicTools.getScreenSize(appData.main)
  private val tmpDeviceResolution = tmpDeviceSize.first * tmpDeviceSize.second.toDouble()
  private fun update(x: Int = -1, y: Int = -1, width: Int = -1, height: Int = -1) {
    if (x != -1) floatWindowParams.x = x
    if (y != -1) floatWindowParams.y = y
    if (width != -1) floatWindowParams.width = width
    if (height != -1) floatWindowParams.height = height
    if (width != -1) {
      // 等比缩放控件大小
      val tmp = sqrt((width * height).toDouble() / tmpDeviceResolution)
      // 整体背景圆角
      val floatVideoShape = floatWindow.floatWindow.background as GradientDrawable
      floatVideoShape.cornerRadius =
        appData.main.resources.getDimension(R.dimen.floatWindowCron) * tmp.toFloat()
      // 上下标题
      val floatVideoTitle1LayoutParams = floatWindow.floatWindowTitle1.layoutParams
      floatVideoTitle1LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatWindowTitle) * tmp).toInt()
      floatWindow.floatWindowTitle1.layoutParams = floatVideoTitle1LayoutParams
      val floatVideoTitle2LayoutParams = floatWindow.floatWindowTitle2.layoutParams
      floatVideoTitle2LayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatWindowTitle) * tmp).toInt()
      floatWindow.floatWindowTitle2.layoutParams = floatVideoTitle2LayoutParams
    }
    appData.main.windowManager.updateViewLayout(floatWindow.root, floatWindowParams)
    floatWindow.floatWindowSurface.doOnPreDraw {
      surfaceSize = Pair(it.width, it.height)
    }
  }

  // 设置全屏
  private fun changeToFull() {
    appData.main.startActivity(Intent(appData.main, MasterActivity::class.java))
    // 屏幕大小
    var screenSize = appData.publicTools.getScreenSize(appData.main)
    if (client!!.videoDecode.videoPortrait xor (screenSize.second > screenSize.first))
      screenSize = Pair(screenSize.second, screenSize.first)
    // 旋转屏幕方向
    appData.main.requestedOrientation =
      if (client!!.videoDecode.videoPortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    // 进入专注模式
    MasterActivity.isFocus = true
    // 隐藏上下栏
    floatWindow.floatWindowTitle1.visibility = View.GONE
    floatWindow.floatWindowTitle2.visibility = View.GONE
    // 设置surface留白
    calculateSurface(screenSize)
    // 显示导航悬浮球
    if (floatBall == null) floatBall = FloatBall(client!!, this)
    floatBall!!.show()
    // 全屏设置
    val flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    floatWindow.root.systemUiVisibility = flags
    // 更新位置大小
    update(x = 0, y = 0, width = screenSize.first, height = screenSize.second)
  }

  // 计算视频上下左右留白
  private fun calculateSurface(screenSize: Pair<Int, Int>) {
    val surfaceLayoutLayoutParams = floatWindow.floatWindowSurfaceLayout.layoutParams
    val surfaceLayoutParams = floatWindow.floatWindowSurface.layoutParams
    val tmp1 =
      client!!.videoDecode.videoSize.second * screenSize.first / client!!.videoDecode.videoSize.first
    // 横向最大不会超出
    if (screenSize.second > tmp1) {
      surfaceLayoutLayoutParams.height = tmp1
      surfaceLayoutParams.width = LayoutParams.MATCH_PARENT
    } else {
      surfaceLayoutLayoutParams.height = LayoutParams.MATCH_PARENT
      surfaceLayoutParams.width =
        client!!.videoDecode.videoSize.first * screenSize.second / client!!.videoDecode.videoSize.second
    }
    floatWindow.floatWindowSurfaceLayout.layoutParams = surfaceLayoutLayoutParams
    floatWindow.floatWindowSurface.layoutParams = surfaceLayoutParams
  }

  // 设置小窗
  private fun changeToSmall() {
    // 屏幕大小
    val screenSize = appData.publicTools.getScreenSize(appData.main)
    // 恢复旧的位置大小--检查是否mini页面期间旋转导致小窗超出
    if (miniSite != null && miniSite!!.first == (appData.main.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)) {
      update(
        miniSite!!.second.first,
        miniSite!!.second.second,
        miniSite!!.third.first,
        miniSite!!.third.second
      )
      miniSite = null
      return
    }
    // 隐藏导航球
    floatBall?.hide()
    // 退出专注模式
    MasterActivity.isFocus = false
    appData.main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    // 显示上下栏
    floatWindow.floatWindowTitle1.visibility = View.VISIBLE
    floatWindow.floatWindowTitle2.visibility = View.VISIBLE
    // 设置surface高度
    var layoutParams = floatWindow.floatWindowSurfaceLayout.layoutParams
    layoutParams.height = LayoutParams.MATCH_PARENT
    floatWindow.floatWindowSurfaceLayout.layoutParams = layoutParams
    layoutParams = floatWindow.floatWindowSurface.layoutParams
    layoutParams.width = LayoutParams.MATCH_PARENT
    floatWindow.floatWindowSurface.layoutParams = layoutParams
    // 取消全屏
    floatWindow.root.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    // 更新位置大小
    autoSite(screenSize)
  }

  // 计算悬浮窗位置
  private fun autoSite(screenSize: Pair<Int, Int>) {
    val tmpWidth: Int
    val tmpHeight: Int
    // 横向最大不会超出
    if (screenSize.second > (client!!.videoDecode.videoSize.second * screenSize.first / client!!.videoDecode.videoSize.first)) {
      tmpWidth = screenSize.first * 3 / 4
      tmpHeight =
        tmpWidth * client!!.videoDecode.videoSize.second / client!!.videoDecode.videoSize.first
    }
    // 竖向最大不会超出
    else {
      tmpHeight = screenSize.second * 3 / 4
      tmpWidth =
        tmpHeight * client!!.videoDecode.videoSize.first / client!!.videoDecode.videoSize.second
    }
    val x = (screenSize.first - tmpWidth) / 2
    val y = (screenSize.second - tmpHeight) / 2
    val titleHeight =
      (appData.main.resources.getDimension(R.dimen.floatWindowTitle) * sqrt((tmpWidth * tmpHeight).toDouble() / tmpDeviceResolution) * 1.6).toInt()
    update(x = x, y = y, width = tmpWidth, height = tmpHeight + titleHeight)
  }


  private var miniSite: Triple<Boolean, Pair<Int, Int>, Pair<Int, Int>>? = null
  private fun changeToMini() {
    miniSite = Triple(
      appData.main.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
      Pair(floatWindowParams.x, floatWindowParams.y),
      Pair(floatWindowParams.width, floatWindowParams.height)
    )
    val size = appData.main.resources.getDimension(R.dimen.floatWindowMini).toInt()
    val screenSize = appData.publicTools.getScreenSize(appData.main)
    floatWindowParams.apply {
      x = screenSize.first - size
      y = (screenSize.second * 0.5).toInt()
      width = size
      height = size
    }
    appData.main.windowManager.updateViewLayout(floatWindow.root, floatWindowParams)
    floatWindow.floatWindowMiniLayout.visibility = View.VISIBLE
  }

  // 检测旋转
  private fun changeRotation() {
    var screenSize = appData.publicTools.getScreenSize(appData.main)
    // 全屏or小窗
    if (MasterActivity.isFocus) {
      // 旋转屏幕方向
      appData.main.requestedOrientation =
        if (client!!.videoDecode.videoPortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      screenSize = Pair(screenSize.second, screenSize.first)
      // 计算Surface留白
      calculateSurface(screenSize)
      update(x = 0, y = 0, width = screenSize.first, height = screenSize.second)
      // 更新悬浮球
      floatBall?.show()
    } else {
      // 更新位置大小
      autoSite(screenSize)
    }
  }

  // 设置焦点监听
  private var keyFocus = false
  private fun setFloatVideoListener() {
    floatWindow.root.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_OUTSIDE -> {
          if (keyFocus) {
            floatWindowParams.flags = floatVideoLayoutParamsFlagNoFocus
            appData.main.windowManager.updateViewLayout(floatWindow.root, floatWindowParams)
            keyFocus = false
          }
        }
      }
      return@setOnTouchListener true
    }
  }

  private fun setMiniViewListener() {
    var xx = 0
    var yy = 0
    var xxx = 0
    var yyy = 0
    val size = appData.main.resources.getDimension(R.dimen.floatWindowMini).toInt()
    val gestureDetector =
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
          floatWindow.floatWindowMiniLayout.visibility = View.GONE
          changeToSmall()
          return super.onSingleTapConfirmed(e)
        }
      })
    floatWindow.floatWindowMiniLayout.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.rawX.toInt()
          yy = event.rawY.toInt()
          xxx = event.x.toInt()
          yyy = event.y.toInt()
          gestureDetector.onTouchEvent(event)
        }

        MotionEvent.ACTION_MOVE -> {
          val x = event.rawX.toInt()
          val y = event.rawY.toInt()
          // 取消手势识别,适配一些机器将点击视作小范围移动(小于4的圆内不做处理)
          if (xx != -1) {
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) < 16) return@setOnTouchListener true
            xx = -1
            event.action = MotionEvent.ACTION_CANCEL
            gestureDetector.onTouchEvent(event)
          }
          // 新位置
          val newX = x - xxx
          val newY = y - yyy
          val screenSize = appData.publicTools.getScreenSize(appData.main)
          // 防止超出屏幕
          if (newX < 0 || newY - 100 < 0 || newX + size > screenSize.first || newY + size + 100 > screenSize.second) return@setOnTouchListener true
          update(x = newX, y = newY)
        }

        else -> {
          gestureDetector.onTouchEvent(event)
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置视频区域触摸监听
  private fun setSurfaceListener() {
    // 视频触摸控制
    val pointerList = Array(20) { 0 }
    floatWindow.floatWindowSurface.setOnTouchListener { _, event ->
      try {
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            client!!.controller.sendTouchEvent(
              MotionEvent.ACTION_DOWN,
              p,
              x,
              y,
              surfaceSize.first,
              surfaceSize.second
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
            client!!.controller.sendTouchEvent(
              MotionEvent.ACTION_UP,
              p,
              x,
              y,
              surfaceSize.first,
              surfaceSize.second
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
              client!!.controller.sendTouchEvent(
                MotionEvent.ACTION_MOVE,
                p,
                x,
                y,
                surfaceSize.first,
                surfaceSize.second
              )
            }
          }
        }
      } catch (_: IllegalArgumentException) {
      }
      // 获取焦点
      if (!keyFocus) {
        floatWindowParams.flags = floatVideoLayoutParamsFlagFocus
        keyFocus = true
        appData.main.windowManager.updateViewLayout(floatWindow.root, floatWindowParams)
      }
      return@setOnTouchListener true
    }
  }

  // 设置按钮监听
  private fun setButtonListener() {
    // 导航按钮
    floatWindow.floatWindowBack.setOnClickListener {
      client!!.controller.sendKeyEvent(4)
    }
    floatWindow.floatWindowHome.setOnClickListener {
      client!!.controller.sendKeyEvent(3)
    }
    floatWindow.floatWindowSwitch.setOnClickListener {
      client!!.controller.sendKeyEvent(187)
    }
    // 全屏按钮
    floatWindow.floatWindowFullscreen.setOnClickListener {
      changeToFull()
    }
    // 最小化按钮
    floatWindow.floatWindowMinscreen.setOnClickListener {
      changeToMini()
    }
    // 悬浮窗大小拖动按钮
    setSetSizeListener()
    // 横条
    setFloatBar()
  }

  // 设置悬浮窗大小拖动按钮监听控制
  private fun setSetSizeListener() {
    val minSize = appData.publicTools.dp2px(150f)
    floatWindow.floatWindowSetSize.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_MOVE -> {
          val tmpWidth = event.rawX - floatWindowParams.x
          val tmpHeight =
            tmpWidth * client!!.videoDecode.videoSize.second / client!!.videoDecode.videoSize.first
          if (tmpWidth < minSize || tmpHeight < minSize) return@setOnTouchListener true
          val titleHeight =
            (appData.main.resources.getDimension(R.dimen.floatWindowTitle) * sqrt((tmpWidth * tmpHeight).toDouble() / tmpDeviceResolution) * 1.6).toInt()
          update(width = tmpWidth.toInt(), height = tmpHeight.toInt() + titleHeight)
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置上横条监听控制
  private fun setFloatBar() {
    var statusBarHeight = 0
    // 获取状态栏高度
    val resourceId = appData.main.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      statusBarHeight = appData.main.resources.getDimensionPixelSize(resourceId)
    }
    var x = 0
    var y = 0
    floatWindow.floatWindowBar.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          x = event.x.toInt()
          y = event.y.toInt()
        }

        MotionEvent.ACTION_MOVE -> {
          // 新位置
          val newX = event.rawX.toInt() - x - floatWindowParams.width / 4
          val newY = event.rawY.toInt() - y
          // 避免移动至状态栏等不可触控区域
          if (newY < statusBarHeight + 10) return@setOnTouchListener true
          update(x = newX, y = newY)
        }
      }
      return@setOnTouchListener true
    }
  }

  private var fps = 0

  @SuppressLint("SetTextI18n")
  override fun handle(mode: Int, arg: Int) {
    appData.mainScope.launch {
      withContext(Dispatchers.Main) {
        when (mode) {
          HandleUi.StartControl -> {
            // 设置焦点监听
            setFloatVideoListener()
            // 设置视频界面触摸监听
            setSurfaceListener()
            // 设置按钮监听
            setButtonListener()
            // 设置横条监听
            setFloatBar()
            // 设置修改大小按钮监听
            setSetSizeListener()
            // 设置Mini界面监听
            setMiniViewListener()
            // 全屏or小窗模式
            if (appData.setting.defaultFull) changeToFull() else changeToSmall()
          }

          HandleUi.StopControl -> hide()
          HandleUi.ChangeRotation -> changeRotation()
          HandleUi.ChangeSmallWindow -> changeToSmall()
          HandleUi.UpdateFps -> fps = arg
          HandleUi.UpdateDelay -> {
            floatWindow.floatWindowInfo.text = "$fps\n${arg}ms"
          }
        }
      }
    }
  }

  companion object {
    val floatType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else WindowManager.LayoutParams.TYPE_PHONE
    private const val baseLayoutParamsFlag =
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    const val floatVideoLayoutParamsFlagFocus =
      baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    const val floatVideoLayoutParamsFlagNoFocus =
      baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
  }

}