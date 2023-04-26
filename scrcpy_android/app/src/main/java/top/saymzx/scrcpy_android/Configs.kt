package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import androidx.lifecycle.ViewModel
import com.tananaev.adblib.AdbStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.*

class Configs : ViewModel() {
  // 是否已经初始化数据
  var isInit = false

  // 被控端IP地址
  var remoteIp = ""

  // 被控端ADB端口
  val remotePort = 5555

  // 被控端socket端口
  val remoteSocketPort = 6006

  // 被控端屏幕大小（缩放后的，即视频流大小）
  var remoteWidth = 0
  var remoteHeight = 0

  // 主控端屏幕大小
  var localWidth = 0
  var localHeight = 0

  // 解码器
  lateinit var videoDecodec: MediaCodec
  lateinit var audioDecodec: MediaCodec

  // 视频编解码器类型
  var videoCodecMime = ""

  // 视频帧率
  var fps = 0

  // 视频码率
  var videoBit = 0

  // 状态标识(-7为关闭状态，小于0为关闭中，0为准备中，1为投屏中)
  var status = -7

  // 连接流
  lateinit var adbStream: AdbStream
  lateinit var videoStream: DataInputStream
  lateinit var audioStream: DataInputStream
  lateinit var controlStream: DataOutputStream

  // 控制队列
  val controls = LinkedList<ByteArray>() as Queue<ByteArray>

  // 悬浮窗
  lateinit var surfaceLayoutParams: WindowManager.LayoutParams

  @SuppressLint("StaticFieldLeak")
  lateinit var surfaceView: SurfaceView

  lateinit var navLayoutParams: WindowManager.LayoutParams

  @SuppressLint("StaticFieldLeak")
  lateinit var navView: View

  // 音频播放器
  lateinit var audioTrack: AudioTrack

  // 音频放大器
  private lateinit var loudnessEnhancer: LoudnessEnhancer

  // context
  @SuppressLint("StaticFieldLeak")
  lateinit var main: MainActivity

  // 初始化
  fun init() {
    // 获取主控端分辨率
    val metric = DisplayMetrics()
    main.windowManager.defaultDisplay.getRealMetrics(metric)
    localWidth = metric.widthPixels
    localHeight = metric.heightPixels
    // 初始化显示悬浮窗
    setSurface()
    // 初始化导航悬浮窗
    setFloatNav()
    // 初始化音频播放器
    setAudioTrack()
  }

  // 初始化显示悬浮窗
  @SuppressLint("ClickableViewAccessibility")
  private fun setSurface() {
    // 创建显示悬浮窗
    surfaceView = SurfaceView(main)
    surfaceLayoutParams = WindowManager.LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
      flags =
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON      //位置大小设置
      gravity = Gravity.START or Gravity.TOP
      x = 0
      y = 0
    }
    // 触摸xy记录（为了适配有些设备过于敏感，将点击识别为小范围移动）
    val pointerList = ArrayList<Int>(20)
    for (i in 1..20) pointerList.add(0)
    surfaceView.setOnTouchListener { _, event ->
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
              x * remoteWidth / localWidth,
              y * remoteHeight / localHeight
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
              MotionEvent.ACTION_UP, p, x * remoteWidth / localWidth, y * remoteHeight / localHeight
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
                MotionEvent.ACTION_MOVE,
                p,
                x * remoteWidth / localWidth,
                y * remoteHeight / localHeight
              )
            }
          }
        }
      } catch (_: IllegalArgumentException) {
      }
      return@setOnTouchListener true
    }
  }

  // 初始化导航悬浮窗
  @SuppressLint("ClickableViewAccessibility", "InflateParams")
  private fun setFloatNav() {
    // 创建导航悬浮窗
    navView = LayoutInflater.from(main).inflate(R.layout.float_nav, null)
    navLayoutParams = WindowManager.LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
      flags =
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE    //位置大小设置
      width = (60 * main.resources.displayMetrics.density + 0.5f).toInt()
      height = (60 * main.resources.displayMetrics.density + 0.5f).toInt()
      gravity = Gravity.START or Gravity.TOP
      format = PixelFormat.RGBA_8888
    }
    val gestureDetector = GestureDetector(main, object : GestureDetector.SimpleOnGestureListener() {
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
    navView.setOnTouchListener { _, event ->
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
          }
          event.action = MotionEvent.ACTION_CANCEL
          gestureDetector.onTouchEvent(event)
          navLayoutParams.x = x - navLayoutParams.width / 2
          navLayoutParams.y = y - navLayoutParams.height / 2
          main.windowManager.updateViewLayout(navView, navLayoutParams)
          return@setOnTouchListener true
        }
        else -> {
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
      }
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
    touchByteBuffer.putInt(x)
    touchByteBuffer.putInt(y)
    // 屏幕尺寸
    touchByteBuffer.putShort(remoteWidth.toShort())
    touchByteBuffer.putShort(remoteHeight.toShort())
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

  // 初始化音频播放器
  private fun setAudioTrack() {
    val sampleRate = 48000
    // 初始化音频播放器
    val minBufferSize = AudioTrack.getMinBufferSize(
      sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
    )
    audioTrack = AudioTrack.Builder().setAudioAttributes(
      AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(
      AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
    ).setBufferSizeInBytes(minBufferSize * 8).build()
    // 声音增强
    try {
      loudnessEnhancer = LoudnessEnhancer(audioTrack.audioSessionId)
      loudnessEnhancer.setTargetGain(4000)
      loudnessEnhancer.enabled = true
    } catch (_: IllegalArgumentException) {
    }
    audioTrack.play()
  }

}