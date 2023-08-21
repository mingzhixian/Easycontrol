package top.saymzx.easycontrol.app.entity

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import top.saymzx.easycontrol.app.R
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.client.Client
import top.saymzx.easycontrol.app.databinding.ModuleFloatNavBinding
import top.saymzx.easycontrol.app.entity.FloatWindow.Companion.floatNavLayoutParamsFlag
import top.saymzx.easycontrol.app.entity.FloatWindow.Companion.floatType

class FloatBall(private val client: Client) {
  // 导航悬浮球
  private var floatNav = ModuleFloatNavBinding.inflate(appData.main.layoutInflater)
  private var floatNavParams = WindowManager.LayoutParams().apply {
    type = floatType
    flags = floatNavLayoutParamsFlag
    gravity = Gravity.START or Gravity.TOP
    val floatNavSize = appData.publicTools.dp2px(appData.setValue.floatNavSize.toFloat()).toInt()
    width = floatNavSize
    height = floatNavSize
    x = 40
    y = (if (client.videoDecode.devicePortrait) appData.deviceHeight else appData.deviceWidth) / 2
    format = PixelFormat.TRANSLUCENT
  }

  // 是否显示
  var hasShow = false

  init {
    // 设置监听
    setFloatNavListener()
  }

  fun show() {
    if (!hasShow) {
      hasShow = true
      appData.main.windowManager.addView(floatNav.root, floatNavParams)
    }
  }

  fun hide() {
    if (hasShow) {
      hasShow = false
      appData.main.windowManager.removeView(floatNav.root)
    }
  }

  // 更新
  private fun update(x: Int = -1, y: Int = -1, width: Int = -1, height: Int = -1) {
    if (hasShow) {
      if (x != -1) floatNavParams.x = x
      if (y != -1) floatNavParams.y = y
      if (width != -1) floatNavParams.width = width
      if (height != -1) floatNavParams.height = height
      appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    }
  }

  // 设置悬浮球点击事件
  private lateinit var gestureDetector: GestureDetector
  private fun setClickEventHandle() {
    gestureDetector =
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
          client.controller.sendKeyEvent(4)
          return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
          client.controller.sendKeyEvent(3)
          return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
          setFloatNavMenuListener()
          super.onLongPress(e)
        }
      })
  }

  // 设置悬浮球移动事件
  @SuppressLint("InflateParams", "ClickableViewAccessibility")
  private fun setTouchEventHandle() {
    var xx = 0
    var yy = 0
    val width = appData.publicTools.dp2px(appData.setValue.floatNavSize.toFloat()).toInt() / 2
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
          update(x = x - width, y = y - width)
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
  private var floatNavSite = Pair(false, 0)
  private fun changeToMenu() {
    // 展示MENU
    floatNav.floatNavMenu.visibility = View.VISIBLE
    floatNav.floatNavImage.visibility = View.GONE
    // 更新位置和大小
    val menuWidth = appData.main.resources.getDimension(R.dimen.floatNavMenuW).toInt()
    update(
      width = menuWidth,
      height = appData.main.resources.getDimension(R.dimen.floatNavMenuH).toInt()
    )
    // 超出屏幕
    val size = appData.publicTools.getScreenSize(appData.main)
    if (floatNavParams.x + menuWidth > size.first) {
      floatNavSite = Pair(true, floatNavParams.x)
      update(x = size.first - menuWidth)
    }
    // 返回导航球
    floatNav.floatNavBack.setOnClickListener {
      backFloatNav()
    }
    // 发送最近任务键
    floatNav.floatNavSwitch.setOnClickListener {
      client.controller.sendKeyEvent(187)
      backFloatNav()
    }
    // 退出全屏
    floatNav.floatNavExitFull.setOnClickListener {
      setSmallWindow()
      update(true)
    }
    // 退出
    floatNav.floatNavExit.setOnClickListener {
      hide()
    }
  }

  // 回到导航球模式
  private fun changeToNav() {
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

}