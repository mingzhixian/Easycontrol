package top.saymzx.easycontrol.app.entity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.*
import androidx.core.view.*
import top.saymzx.easycontrol.app.FullScreenActivity
import top.saymzx.easycontrol.app.R
import top.saymzx.easycontrol.app.appData
import top.saymzx.easycontrol.app.client.Client
import top.saymzx.easycontrol.app.client.HandleUi
import top.saymzx.easycontrol.app.databinding.ModuleFloatVideoBinding
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt


@SuppressLint("ClickableViewAccessibility", "InternalInsetResource", "DiscouragedApi")
class FloatWindow(private val device: Device) : HandleUi {

  // Client
  private lateinit var client: Client

  // 悬浮窗
  private lateinit var floatVideo: ModuleFloatVideoBinding
  private lateinit var floatVideoParams: WindowManager.LayoutParams

  // 小窗大小
  private lateinit var floatVideoSize: Pair<Int, Int>


  // 显示悬浮窗
  init {
    // 创建悬浮窗
    floatVideo = ModuleFloatVideoBinding.inflate(appData.main.layoutInflater)
    WindowManager.LayoutParams().apply {
      type = floatType
      flags = floatVideoLayoutParamsFlagFocus
      gravity = Gravity.START or Gravity.TOP
      format = PixelFormat.TRANSLUCENT
    }
    appData.main.windowManager.addView(floatVideo.root, floatVideoParams)
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
    // 全屏or小窗模式
    if (appData.setValue.defaultFull) setFull() else setSmallWindow()
    update(true)
  }

  // 隐藏悬浮窗
  fun hide() {
    client.isNormal = false
    appData.main.windowManager.removeView(floatVideo.root)
  }

  // 更新悬浮窗
  private fun update(hasChangeSize: Boolean) {
    appData.main.windowManager.updateViewLayout(
      floatVideo.root, floatVideoParams
    )
    try {
      appData.main.windowManager.updateViewLayout(floatNav.root, floatNavParams)
    } catch (_: Exception) {
    }
    // 重新计算控件大小
    if (hasChangeSize) {
      // 等比缩放控件大小
      val tmp =
        sqrt((floatVideoSize.first * floatVideoSize.second).toDouble() / (appData.deviceWidth * appData.deviceHeight).toDouble())
      // 整体背景圆角
      val floatVideoShape = floatVideo.floatVideo.background as GradientDrawable
      floatVideoShape.cornerRadius =
        appData.main.resources.getDimension(R.dimen.floatVideoBackground) * tmp.toFloat()
      // 上下标题
      val floatVideoTitle = getViewsByTag(floatVideo.root, "float_video_title")
      val floatVideoTitleLayoutParams = floatVideoTitle[0].layoutParams
      floatVideoTitleLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      for (i in floatVideoTitle) i.layoutParams = floatVideoTitleLayoutParams
      // 横条
      val floatVideoBarLayoutParams = floatVideo.floatVideoBar.layoutParams
      floatVideoBarLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      floatVideo.floatVideoBar.layoutParams = floatVideoBarLayoutParams
      floatVideo.floatVideoBar.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoBarPadding) * tmp).toInt())
      // 按钮
      val floatVideoButton = getViewsByTag(floatVideo.root, "float_video_button")
      val floatVideoButtonLayoutParams = floatVideoTitle[0].layoutParams
      floatVideoButtonLayoutParams.height =
        (appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt()
      for (i in floatVideoButton) {
        i.layoutParams = floatVideoButtonLayoutParams
        i.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoButtonPadding) * tmp).toInt())
      }
      // 刷新率
      floatVideo.floatVideoInfo.setPadding((appData.main.resources.getDimension(R.dimen.floatVideoTitle) * tmp).toInt())
    }
  }

  // 设置全屏
  private fun setFull() {
    // 旋转屏幕方向
    appData.fullScreenOrientation =
      if (client.videoDecode.devicePortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    // 进入专注模式
    appData.isFocus = true
    appData.main.startActivity(Intent(appData.main, FullScreenActivity::class.java))
    // 更新悬浮窗
    if (client.videoDecode.devicePortrait) {
      calculateFloatSize(appData.deviceWidth, appData.deviceHeight, false)
    } else {
      calculateFloatSize(appData.deviceHeight, appData.deviceWidth, false)
    }
    floatVideoParams.apply {
      x = 0
      y = 0
    }
    // 显示导航悬浮球
    showFloatNav()
    // 隐藏上下栏
    val floatVideoTitle = getViewsByTag(floatVideo.root, "float_video_title")
    for (i in floatVideoTitle) i.visibility = View.GONE
    update(true)
  }

  // 设置小窗
  private fun setSmallWindow() {
    // 隐藏导航球
    hideFloatNav()
    // 计算位置和大小
    calculateFloatSite(client.videoDecode.videoSize.first, client.videoDecode.videoSize.second)
    // 显示上下栏
    val floatVideoTitle = getViewsByTag(floatVideo.root, "float_video_title")
    for (i in floatVideoTitle) i.visibility = View.VISIBLE
    update(true)
  }

  // 检测旋转
  fun changeRotation() {
    // 全屏or小窗
    if (getViewsByTag(floatVideo.root, "float_video_title")[0].visibility == View.GONE) {
      // 旋转屏幕方向
      appData.fullScreenOrientation =
        if (client.videoDecode.devicePortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      // 导航球
      floatNavParams.apply {
        x = 40
        y =
          (if (client.videoDecode.devicePortrait) appData.deviceHeight else appData.deviceWidth) / 2
      }
      floatVideoSize = Pair(floatVideoSize.second, floatVideoSize.first)
      // 更新悬浮窗
      floatVideoParams.apply {
        width = floatVideoSize.first
        height = floatVideoSize.second
      }
    } else {
      // 计算位置和大小
      calculateFloatSite(remoteVideoWidth, remoteVideoHeight)
    }
    update(true)
  }

  // 设置焦点监听
  private var isFocus = false
  private fun setFloatVideoListener() {
    floatVideo.root.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_OUTSIDE -> {
          if (isFocus) {
            floatVideoParams.flags = floatVideoLayoutParamsFlagNoFocus
            update(false)
            isFocus = false
          }
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
      try {
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            client.controller.sendTouchEvent(
              MotionEvent.ACTION_DOWN,
              p,
              x,
              y,
              floatVideoSize.first,
              floatVideoSize.second
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
            client.controller.sendTouchEvent(
              MotionEvent.ACTION_UP,
              p,
              x,
              y,
              floatVideoSize.first,
              floatVideoSize.second
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
              client.controller.sendTouchEvent(
                MotionEvent.ACTION_MOVE,
                p,
                x,
                y,
                floatVideoSize.first,
                floatVideoSize.second
              )
            }
          }
        }
      } catch (_: IllegalArgumentException) {
      }
      // 获取焦点
      if (!isFocus) {
        floatVideoParams.flags = floatVideoLayoutParamsFlagFocus
        update(false)
        isFocus = true
      }
      return@setOnTouchListener true
    }
  }

  // 设置按钮监听
  private fun setButtonListener() {
    // 导航按钮
    floatVideo.floatVideoBack.setOnClickListener {
      client.controller.sendKeyEvent(4)
    }
    floatVideo.floatVideoHome.setOnClickListener {
      client.controller.sendKeyEvent(3)
    }
    floatVideo.floatVideoSwitch.setOnClickListener {
      client.controller.sendKeyEvent(187)
    }
    // 全屏按钮
    floatVideo.floatVideoFullscreen.setOnClickListener {
      setFull()
      update(true)
    }
    // 最小化按钮
    floatVideo.floatVideoMinscreen.setOnClickListener {
      setSmallSmall()
      update(true)
    }
    // 关闭按钮
    floatVideo.floatVideoStop.setOnClickListener {
      hide()
    }
    // 悬浮窗大小拖动按钮
    setSetSizeListener()
    // 横条
    setFloatBar()
  }

  // 设置悬浮窗大小拖动按钮监听控制
  private fun setSetSizeListener() {
    var width = 0f
    val maxCal = appData.publicTools.dp2px(30f)
    floatVideo.floatVideoSetSize.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          width = event.rawX - floatVideoParams.x
        }

        MotionEvent.ACTION_MOVE -> {
          // 计算新大小（等比缩放）
          val tmpWidth = event.rawX - floatVideoParams.x
          val tmpHeight = tmpWidth * floatVideoSize.second / floatVideoSize.first
          // 最小300个像素
          if (tmpWidth < 300 || tmpHeight < 300) return@setOnTouchListener true
          calculateFloatSize(tmpWidth.toInt(), tmpHeight.toInt(), true)
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
    floatVideo.floatVideoBar.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          x = event.x.toInt()
          y = event.y.toInt()
        }

        MotionEvent.ACTION_MOVE -> {
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
      return@setOnTouchListener true
    }
  }


  // 计算悬浮窗大小
  private fun calculateFloatSize(tmpWidth: Int, tmpHeight: Int, hasTitle: Boolean) {
    floatVideoSize = Pair(tmpWidth, tmpHeight)
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
      val tmp2Height = tmp2Width * floatVideoSize.second / floatVideoSize.first
      calculateFloatSize(tmp2Width, tmp2Height, true)
    }
    // 竖向最大不会超出
    else {
      val tmp2Height = screenSize.second * 3 / 4
      val tmp2Width = tmp2Height * floatVideoSize.first / floatVideoSize.second
      calculateFloatSize(tmp2Width, tmp2Height, true)
    }
    // 居中显示
    floatVideoParams.apply {
      x = (screenSize.first - width) / 2
      y = (screenSize.second - height) / 2
    }
  }

  // 查找所有具有相同tag的元素
  private fun getViewsByTag(root: ViewGroup, tag: String): ArrayList<View> {
    val views = ArrayList<View>()
    val childCount = root.childCount
    for (i in 0 until childCount) {
      val child = root.getChildAt(i)
      if (child is ViewGroup) {
        views.addAll(getViewsByTag(child, tag))
      }
      val tagObj = child.tag
      if (tagObj != null && tagObj == tag) {
        views.add(child)
      }
    }
    return views
  }

  override fun handle(mode: Int, arg: Int) {
  }

  companion object {
    val floatType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else WindowManager.LayoutParams.TYPE_PHONE
    private val baseLayoutParamsFlag =
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    val floatNavLayoutParamsFlag =
      baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    val floatVideoLayoutParamsFlagNoFocus =
      baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    val floatVideoLayoutParamsFlagFocus =
      baseLayoutParamsFlag or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
  }

}