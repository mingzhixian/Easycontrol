package top.saymzx.easycontrol.app.entity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import top.saymzx.easycontrol.app.R
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.client.Client
import top.saymzx.easycontrol.app.client.HandleUi
import top.saymzx.easycontrol.app.databinding.ModuleFloatBallBinding
import top.saymzx.easycontrol.app.entity.FloatWindow.Companion.floatType


class FloatBall(private val client: Client, private val handleUi: HandleUi) {
  // 导航悬浮球
  private var floatBall = ModuleFloatBallBinding.inflate(appData.main.layoutInflater)
  private val floatBallSize = appData.publicTools.dp2px(appData.setting.floatBallSize.toFloat())
  private var floatBallParams = WindowManager.LayoutParams().apply {
    type = floatType
    flags =
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    gravity = Gravity.START or Gravity.TOP
    format = PixelFormat.TRANSLUCENT
  }

  // 是否显示
  private var hasShow = false

  // 是否为球体状态
  private var isBall = true

  init {
    setClickEventHandle()
    setTouchEventHandle()
  }

  fun show() {
    if (!hasShow) {
      hasShow = true
      appData.main.windowManager.addView(floatBall.root, floatBallParams)
    }
    var screenSize = appData.publicTools.getScreenSize(appData.main)
    if (client.videoDecode.videoPortrait xor (screenSize.second > screenSize.first))
      screenSize = Pair(screenSize.second, screenSize.first)
    update(x = 40, y = screenSize.second / 2, width = floatBallSize, height = floatBallSize)
  }

  fun hide() {
    if (hasShow) {
      hasShow = false
      appData.main.windowManager.removeView(floatBall.root)
    }
  }

  // 更新
  private fun update(x: Int = -1, y: Int = -1, width: Int = -1, height: Int = -1) {
    if (hasShow) {
      if (x != -1) floatBallParams.x = x
      if (y != -1) floatBallParams.y = y
      if (width != -1) floatBallParams.width = width
      if (height != -1) floatBallParams.height = height
      appData.main.windowManager.updateViewLayout(floatBall.root, floatBallParams)
    }
  }

  // 设置悬浮球点击事件
  private lateinit var gestureDetector: GestureDetector
  private fun setClickEventHandle() {
    gestureDetector =
      GestureDetector(appData.main, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
          changeToMenu()
          return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
          changeToMenu()
          super.onLongPress(e)
        }
      })
    // 返回
    floatBall.floatBallBack.setOnClickListener {
      client.controller.sendKeyEvent(4)
      changeToBall()
    }
    // 主页
    floatBall.floatBallHome.setOnClickListener {
      client.controller.sendKeyEvent(3)
      changeToBall()
    }
    // 最近任务
    floatBall.floatBallSwitch.setOnClickListener {
      client.controller.sendKeyEvent(187)
      changeToBall()
    }
    // 退出全屏
    floatBall.floatBallExitFull.setOnClickListener {
      handleUi.handle(HandleUi.ChangeSmallWindow, 0)
      changeToBall()
    }
    // 退出
    floatBall.floatBallExit.setOnClickListener {
      handleUi.handle(HandleUi.StopControl, 0)
    }
  }

  // 设置悬浮球移动事件
  @SuppressLint("InflateParams", "ClickableViewAccessibility")
  private fun setTouchEventHandle() {
    var xx = 0
    var yy = 0
    floatBall.root.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_OUTSIDE -> {
          if (!isBall) changeToBall()
        }
      }
      return@setOnTouchListener true
    }
    floatBall.floatBallBall.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xx = event.rawX.toInt()
          yy = event.rawY.toInt()
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
          update(x = x - floatBallSize / 2, y = y - floatBallSize / 2)
        }

        else -> {
          gestureDetector.onTouchEvent(event)
        }
      }
      return@setOnTouchListener true
    }
  }

  // 设置导航球菜单监听
  private var floatBallSite = Pair(false, 0)
  private fun changeToMenu() {
    isBall = false
    // 动画
    val menuWidth = appData.main.resources.getDimension(R.dimen.floatBallW).toInt()
    val menuHeight = appData.main.resources.getDimension(R.dimen.floatBallH).toInt()
    val tmpWidth = menuWidth - floatBallSize
    val tmpHeight = menuHeight - floatBallSize
    val animator = ValueAnimator.ofFloat(0f, 1f)
    animator.interpolator = OvershootInterpolator()
    animator.duration = 250
    animator.addUpdateListener { valueAnimator ->
      val animatedValue = valueAnimator.animatedValue as Float
      update(
        width = (floatBallSize + tmpWidth * animatedValue).toInt(),
        height = (floatBallSize + tmpHeight * animatedValue).toInt()
      )
      if (animatedValue > 0.3f) {
        if (floatBall.floatBallBall.visibility != View.GONE) {
          floatBall.floatBallBall.visibility = View.GONE
          floatBall.floatBallMenu.visibility = View.VISIBLE
        }
      }
    }
    animator.start()
  }

  // 回到导航球模式
  private fun changeToBall() {
    isBall = true
    // 动画
    val menuWidth = appData.main.resources.getDimension(R.dimen.floatBallW).toInt()
    val menuHeight = appData.main.resources.getDimension(R.dimen.floatBallH).toInt()
    val tmpWidth = menuWidth - floatBallSize
    val tmpHeight = menuHeight - floatBallSize
    val animator = ValueAnimator.ofFloat(0f, 1f)
    animator.interpolator = OvershootInterpolator(1f)
    animator.duration = 250
    animator.addUpdateListener { valueAnimator ->
      val animatedValue = valueAnimator.animatedValue as Float
      update(
        width = (menuWidth - tmpWidth * animatedValue).toInt(),
        height = (menuHeight - tmpHeight * animatedValue).toInt()
      )
      if (animatedValue > 0.7f) {
        if (floatBall.floatBallMenu.visibility != View.GONE) {
          floatBall.floatBallBall.visibility = View.VISIBLE
          floatBall.floatBallMenu.visibility = View.GONE
        }
      }
    }
    animator.start()
  }

}