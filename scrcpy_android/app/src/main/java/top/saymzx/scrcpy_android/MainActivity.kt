package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.Intent.*
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.media.*
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.KeyEvent.*
import android.view.MotionEvent.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.bind.DatatypeConverter
import kotlin.math.abs


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
  var remoteHeight = 1920

  // 主控端屏幕大小
  var localWidth = 0
  var localHeight = 0

  // 解码器
  lateinit var videoDecodec: MediaCodec
  lateinit var audioDecodec: MediaCodec

  // 视频编解码器类型
  var videoCodecMime = "h264"

  // 视频帧率
  var fps = 30

  // 视频码率
  var videoBit = 12000000

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
  lateinit var surfaceLayoutParams: LayoutParams

  @SuppressLint("StaticFieldLeak")
  lateinit var surfaceView: SurfaceView

  lateinit var navLayoutParams: LayoutParams

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
    surfaceLayoutParams = LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LayoutParams.TYPE_APPLICATION_OVERLAY
        else LayoutParams.TYPE_PHONE
      flags =
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or LayoutParams.FLAG_KEEP_SCREEN_ON      //位置大小设置
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
          MotionEvent.ACTION_DOWN, ACTION_POINTER_DOWN -> {
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
          MotionEvent.ACTION_UP, ACTION_POINTER_UP, ACTION_CANCEL -> {
            val i = event.actionIndex
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()
            val p = event.getPointerId(i)
            packTouchControl(
              MotionEvent.ACTION_UP,
              p,
              x * remoteWidth / localWidth,
              y * remoteHeight / localHeight
            )
          }
          ACTION_MOVE -> {
            for (i in 0 until event.pointerCount) {
              val x = event.getX(i).toInt()
              val y = event.getY(i).toInt()
              val p = event.getPointerId(i)
              // 适配一些机器将点击视作小范围移动(小于3的圆内不做处理)
              if (pointerList[p] != -1) {
                if ((pointerList[p] - x) * (pointerList[p] - x) + (pointerList[10 + p] - y) * (pointerList[10 + p] - y) < 9) {
                  return@setOnTouchListener true
                }
                pointerList[p] = -1
              }
              packTouchControl(
                ACTION_MOVE,
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
    navLayoutParams = LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LayoutParams.TYPE_APPLICATION_OVERLAY
        else LayoutParams.TYPE_PHONE
      flags =
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_NOT_FOCUSABLE    //位置大小设置
      width = (60 * main.resources.displayMetrics.density + 0.5f).toInt()
      height = (60 * main.resources.displayMetrics.density + 0.5f).toInt()
      gravity = Gravity.START or Gravity.TOP
      format = PixelFormat.RGBA_8888
    }
    // 手势处理
    var xy = 0
    val gestureDetector = GestureDetector(main, object : GestureDetector.SimpleOnGestureListener() {
      override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        packNavControl(KeyEvent.ACTION_DOWN, KEYCODE_BACK)
        packNavControl(KeyEvent.ACTION_UP, KEYCODE_BACK)
        return super.onSingleTapConfirmed(e)
      }

      override fun onDoubleTap(e: MotionEvent): Boolean {
        packNavControl(KeyEvent.ACTION_DOWN, KEYCODE_HOME)
        packNavControl(KeyEvent.ACTION_UP, KEYCODE_HOME)
        return super.onDoubleTap(e)
      }

      override fun onLongPress(e: MotionEvent) {
        packNavControl(KeyEvent.ACTION_DOWN, KEYCODE_APP_SWITCH)
        packNavControl(KeyEvent.ACTION_UP, KEYCODE_APP_SWITCH)
        super.onLongPress(e)
      }

    })
    navView.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          xy = event.rawX.toInt() + event.rawY.toInt()
          gestureDetector.onTouchEvent(event)
          return@setOnTouchListener true
        }
        ACTION_MOVE -> {
          val x = event.rawX.toInt()
          val y = event.rawY.toInt()
          // 取消手势识别(适配某些设备太敏感)
          if (abs(xy - x - y) < 16) {
            return@setOnTouchListener true
          } else {
            event.action = ACTION_CANCEL
            gestureDetector.onTouchEvent(event)
            navLayoutParams.x = x - navLayoutParams.width / 2
            navLayoutParams.y = y - navLayoutParams.height / 2
            main.windowManager.updateViewLayout(navView, navLayoutParams)
            return@setOnTouchListener true
          }
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
    val byteBuffer = ByteBuffer.allocate(32)
    byteBuffer.clear()
    // 触摸事件
    byteBuffer.put(2)
    // 触摸类型
    byteBuffer.put(action.toByte())
    // pointerId
    byteBuffer.putLong(p.toLong())
    // 坐标位置
    byteBuffer.putInt(x)
    byteBuffer.putInt(y)
    // 屏幕尺寸
    byteBuffer.putShort(remoteWidth.toShort())
    byteBuffer.putShort(remoteHeight.toShort())
    // 按压力度presureInt和buttons
    byteBuffer.putShort(1)
    byteBuffer.putInt(0)
    byteBuffer.putInt(0)
    byteBuffer.flip()
    controls.offer(byteBuffer.array())
  }

  // 组装导航报文
  private fun packNavControl(action: Int, key: Int) {
    val byteBuffer = ByteBuffer.allocate(14)
    byteBuffer.clear()
    // 输入事件
    byteBuffer.put(0)
    // 事件类型
    byteBuffer.put(action.toByte())
    // 按键类型
    byteBuffer.putInt(key)
    // 重复次数
    byteBuffer.putInt(0)
    byteBuffer.putInt(0)
    byteBuffer.flip()
    controls.offer(byteBuffer.array())
  }

  // 初始化音频播放器
  private fun setAudioTrack() {
    val sampleRate = 44100
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

class MainActivity : AppCompatActivity() {

  lateinit var configs: Configs

  // 创建界面
  @SuppressLint("InflateParams")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // viewModel
    configs = ViewModelProvider(this)[Configs::class.java]
    // 全屏显示
    setFullScreen()
    // 冷启动
    if (!configs.isInit) {
      // 注册广播用以关闭程序
      val filter = IntentFilter()
      filter.addAction(ACTION_SCREEN_OFF)
      filter.addAction("top.saymzx.notification")
      registerReceiver(ScreenReceiver(), filter)
      // 检查悬浮窗权限
      if (!Settings.canDrawOverlays(this)) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
      }
      configs.isInit = true
      configs.main = this
      configs.init()
    }
    // 读取数据库并展示设备列表
    setDevicesList()
  }

  // 防止全屏状态失效
  override fun onResume() {
    setFullScreen()
    super.onResume()
  }

  // 自动恢复界面
  override fun onPause() {
    super.onPause()
    if (configs.status == 1) {
      startActivity(intent)
    }
  }

  // 设置全屏显示
  private fun setFullScreen() {
    // 全屏显示
    window.decorView.systemUiVisibility =
      (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  // 读取数据库并展示设备列表
  private fun setDevicesList() {
    val devices = findViewById<RecyclerView>(R.id.devices)
    val deviceAdapter = DeviceAdapter(this)
    devices.layoutManager = LinearLayoutManager(this)
    devices.adapter = deviceAdapter
    // 添加设备
    findViewById<TextView>(R.id.add_device).setOnClickListener {
      val addDeviceView = LayoutInflater.from(this).inflate(R.layout.add_device, null, false)
      val builder: AlertDialog.Builder = AlertDialog.Builder(this)
      builder.setView(addDeviceView)
      builder.setCancelable(false)
      val dialog = builder.create()
      dialog.setCanceledOnTouchOutside(true)
      dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
      addDeviceView.findViewById<Button>(R.id.add_device_ok).setOnClickListener {
        deviceAdapter.newDevice(
          addDeviceView.findViewById<EditText>(R.id.add_device_name).text.toString(),
          addDeviceView.findViewById<EditText>(R.id.add_device_ip).text.toString()
            .replace("\\s|\\n|\\r|\\t".toRegex(), ""),
          addDeviceView.findViewById<Spinner>(R.id.add_device_videoCodec).selectedItem.toString(),
          addDeviceView.findViewById<Spinner>(R.id.add_device_resolution).selectedItem.toString()
            .toInt(),
          addDeviceView.findViewById<Spinner>(R.id.add_device_fps).selectedItem.toString()
            .toInt(),
          addDeviceView.findViewById<Spinner>(R.id.add_device_video_bit).selectedItem.toString()
            .toInt()
        )
        dialog.cancel()
      }
      dialog.show()
    }
  }

  // 开始投屏
  @SuppressLint("SourceLockedOrientationActivity")
  fun startScrcpy() {
    // 子线程发送并连接server
    Thread {
      // 发送server
      sendServer()
      // 连接server
      connectServer {
        // 配置视频解码器
        setVideoDecodec()
        val videoInputThread = Thread { decodecInput("video") }
        videoInputThread.priority = 10
        videoInputThread.start()
        val videoOutputThread = Thread { decodecOutput("video") }
        videoOutputThread.priority = 10
        videoOutputThread.start()
        // 配置音频解码器
        if (setAudioDecodec()) {
          val audioInputThread = Thread { decodecInput("audio") }
          audioInputThread.priority = 8
          audioInputThread.start()
          val audioOutputThread = Thread { decodecOutput("audio") }
          audioOutputThread.priority = 8
          audioOutputThread.start()
        }
        val controlOutputThread = Thread { controlOutput() }
        controlOutputThread.priority = 5
        controlOutputThread.start()
        val notOffScreenThread = Thread { notOffScreen() }
        notOffScreenThread.priority = 1
        notOffScreenThread.start()
        // 设置被控端熄屏
        setPowerOff()
        // 设置状态为投屏中
        configs.status = 1
      }
    }.start()
    // 初始状态为竖屏
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    // 全屏显示
    setFullScreen()
    // 设置显示悬浮窗
    // 防止横屏打开的问题
    if (configs.localWidth > configs.localHeight) {
      val tmp = configs.localWidth
      configs.localWidth = configs.localHeight
      configs.localHeight = tmp
    }
    configs.surfaceLayoutParams.width = configs.localWidth
    configs.surfaceLayoutParams.height = configs.localHeight
    windowManager.addView(configs.surfaceView, configs.surfaceLayoutParams)
    // 设置导航悬浮窗
    configs.navLayoutParams.x = 0
    configs.navLayoutParams.y = configs.localHeight * 3 / 8
    windowManager.addView(configs.navView, configs.navLayoutParams)
    // 设置常驻通知栏用以关闭
    setNotification()
  }

  // 发送server
  private fun sendServer() {
    // 读取或创建保存配对密钥
    val public = File(this.applicationContext.filesDir, "public.key")
    val private = File(this.applicationContext.filesDir, "private.key")
    val crypto: AdbCrypto
    if (public.isFile && private.isFile) {
      crypto = AdbCrypto.loadAdbKeyPair({ data: ByteArray? ->
        DatatypeConverter.printBase64Binary(data)
      }, private, public)
    } else {
      crypto = AdbCrypto.generateAdbKeyPair { data -> DatatypeConverter.printBase64Binary(data) }
      crypto.saveAdbKeyPair(private, public)
    }
    // 连接ADB
    val socket = Socket()
    try {
      socket.connect(InetSocketAddress(configs.remoteIp, configs.remotePort), 1000)
    } catch (_: IOException) {
      runOnUiThread { Toast.makeText(this, "连接失败，请检查IP地址以及是否开启ADB网络调试", Toast.LENGTH_SHORT).show() }
      // 删除导航悬浮窗
      windowManager.removeView(configs.navView)
      // 删除显示悬浮窗
      windowManager.removeView(configs.surfaceView)
      // 取消强制旋转
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      // 恢复为未投屏状态
      configs.status = -7
    }
    val connection = AdbConnection.create(socket, crypto)
    connection.connect()
    configs.adbStream = connection.open("shell:")
    // 删除旧进程
    configs.adbStream.write(" ps -ef | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9 \n")
    // 初始设置
    configs.adbStream.write(" wm size " + configs.localWidth + "x" + configs.localHeight + '\n')
    // 快速启动
    val versionCode = BuildConfig.VERSION_CODE
    configs.adbStream.write(" ls -l /data/local/tmp/scrcpy-server$versionCode.jar \n")
    var str = ""
    for (i in 1..30) {
      str += String(configs.adbStream.read())
      if (str.contains(Regex("shell shell.*scrcpy-server$versionCode.jar"))) break
      else if (str.contains("No such file or directory")) {
        // 读取scrcpy server
        val assetManager = assets
        val serverFileBase64: ByteArray?
        val inputStream = assetManager.open("scrcpy-server.jar")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        serverFileBase64 = Base64.encode(buffer, 2)
        // 发送文件
        configs.adbStream.write(" cd /data/local/tmp \n")
        configs.adbStream.write(" rm serverBase64 \n")
        var serverBase64part: String
        val len: Int = serverFileBase64.size
        var sourceOffset = 0
        while (sourceOffset < len) {
          if (len - sourceOffset >= 4056) {
            val filePart = ByteArray(4056)
            System.arraycopy(serverFileBase64!!, sourceOffset, filePart, 0, 4056)
            sourceOffset += 4056
            serverBase64part = String(filePart, StandardCharsets.US_ASCII)
          } else {
            val rem = len - sourceOffset
            val remPart = ByteArray(rem)
            System.arraycopy(serverFileBase64!!, sourceOffset, remPart, 0, rem)
            sourceOffset += rem
            serverBase64part = String(remPart, StandardCharsets.US_ASCII)
          }
          configs.adbStream.write(" echo $serverBase64part >> serverBase64\n")
        }
        configs.adbStream.write(" base64 -d < serverBase64 > scrcpy-server$versionCode.jar && rm serverBase64 \n")
        Thread.sleep(100)
        break
      }
    }
    configs.adbStream.write(" CLASSPATH=/data/local/tmp/scrcpy-server$versionCode.jar app_process / com.genymobile.scrcpy.Server 2.0 video_codec=${configs.videoCodecMime} max_size=${configs.remoteHeight} video_bit_rate=${configs.videoBit} max_fps=${configs.fps} > /dev/null 2>&1 & \n")
  }

  // 连接server
  private fun connectServer(callback: () -> Unit) {
    var videoSocket: Socket? = null
    var audioSocket: Socket? = null
    var controlSocket: Socket? = null
    // 尝试连接server
    for (i in 1..100) {
      // 中途关闭
      if (configs.status < 0) {
        // 删除旧进程
        configs.adbStream.write(" ps -ef | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9 \n")
        configs.adbStream.close()
        // 删除导航悬浮窗
        windowManager.removeView(configs.navView)
        // 删除显示悬浮窗
        windowManager.removeView(configs.surfaceView)
        // 取消强制旋转
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // 恢复为未投屏状态
        configs.status = -7
      }
      try {
        if (videoSocket == null) videoSocket = Socket(configs.remoteIp, configs.remoteSocketPort)
        if (audioSocket == null) audioSocket = Socket(configs.remoteIp, configs.remoteSocketPort)
        controlSocket = Socket(configs.remoteIp, configs.remoteSocketPort)
        break
      } catch (_: IOException) {
        Log.e("scrcpy", "连接server失败，再次尝试")
        Thread.sleep(80)
      }
    }
    // 连接失败
    if (videoSocket == null || controlSocket == null) {
      runOnUiThread { Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show() }
      // 删除旧文件
      val versionCode = BuildConfig.VERSION_CODE
      configs.adbStream.write(" rm /data/local/tmp/scrcpy-server$versionCode.jar \n")
      // 删除旧进程
      configs.adbStream.write(" ps -ef | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9 \n")
      configs.adbStream.close()
      // 删除导航悬浮窗
      windowManager.removeView(configs.navView)
      // 删除显示悬浮窗
      windowManager.removeView(configs.surfaceView)
      // 取消强制旋转
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      // 恢复为未投屏状态
      configs.status = -7
    }
    configs.videoStream = DataInputStream(videoSocket?.getInputStream())
    configs.audioStream = DataInputStream(audioSocket?.getInputStream())
    configs.controlStream = DataOutputStream(controlSocket?.getOutputStream())
    callback()
  }

  // 视频解码器
  private fun setVideoDecodec() {
    // CodecMeta
    configs.videoStream.readInt()
    configs.remoteWidth = configs.videoStream.readInt()
    configs.remoteHeight = configs.videoStream.readInt()
    // 创建解码器
    val codecMime =
      if (configs.videoCodecMime == "h265") MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
    configs.videoDecodec = MediaCodec.createDecoderByType(codecMime)
    val mediaFormat =
      MediaFormat.createVideoFormat(codecMime, configs.remoteWidth, configs.remoteHeight)
    // 获取视频标识头
    val csd0 = readFrame(configs.videoStream)
    val csd1 = readFrame(configs.videoStream)
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1))
    // 配置解码器
    configs.videoDecodec.configure(mediaFormat, configs.surfaceView.holder.surface, null, 0)
    // 启动解码器
    configs.videoDecodec.start()
    // 解析首帧，解决开始黑屏问题
    var inIndex: Int
    do {
      inIndex = configs.videoDecodec.dequeueInputBuffer(0)
    } while (inIndex < 0)
    configs.videoDecodec.getInputBuffer(inIndex)!!.put(csd0)
    configs.videoDecodec.queueInputBuffer(inIndex, 0, csd0.size, 0, 0)
    do {
      inIndex = configs.videoDecodec.dequeueInputBuffer(0)
    } while (inIndex < 0)
    configs.videoDecodec.getInputBuffer(inIndex)!!.put(csd1)
    configs.videoDecodec.queueInputBuffer(inIndex, 0, csd1.size, 0, 0)
  }

  // 音频解码器
  private fun setAudioDecodec(): Boolean {
    // 是否不支持音频（安卓11以下不支持）
    if (configs.audioStream.readInt() == 0) return false
    // 创建音频解码器
    configs.audioDecodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
    val sampleRate = 44100
    val channelCount = 2
    val bitRate = 128000
    val mediaFormat =
      MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channelCount)
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
    // 获取音频标识头
    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(readFrame(configs.audioStream)))
    // csd1和csd2暂时没用到，所以默认全是用0
    val csd12bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val csd12ByteBuffer = ByteBuffer.wrap(csd12bytes, 0, csd12bytes.size)
    mediaFormat.setByteBuffer("csd-1", csd12ByteBuffer)
    mediaFormat.setByteBuffer("csd-2", csd12ByteBuffer)
    // 配置解码器
    configs.audioDecodec.configure(mediaFormat, null, null, 0)
    // 启动解码器
    configs.audioDecodec.start()
    return true
  }

  // 输入
  private fun decodecInput(mode: String) {
    var inIndex: Int
    val stream = if (mode == "video") configs.videoStream else configs.audioStream
    val decodec = if (mode == "video") configs.videoDecodec else configs.audioDecodec
    // 开始解码
    try {
      while (configs.status == 0) Thread.sleep(20)
      while (configs.status == 1) {
        // 向缓冲区输入数据帧
        val buffer = readFrame(stream)
        // 找到一个空的输入缓冲区
        do {
          inIndex = decodec.dequeueInputBuffer(0)
        } while (inIndex < 0)
        decodec.getInputBuffer(inIndex)!!.put(buffer)
        // 提交解码器解码
        decodec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
      }
    } finally {
      stream.close()
      // 神奇不，try里面套try，可是我不知道为什么有时会莫名其妙崩溃，所以直接忽略，哈哈哈
      try {
        decodec.stop()
        decodec.release()
      } catch (_: IllegalStateException) {
      }
      configs.status--
    }
  }

  // 输出
  private fun decodecOutput(mode: String) {
    var outIndex: Int
    val bufferInfo = BufferInfo()
    val decodec = if (mode == "video") configs.videoDecodec else configs.audioDecodec
    try {
      while (configs.status == 0) Thread.sleep(20)
      if (mode == "video") {
        var decodeNum = 0
        while (configs.status == 1) {
          // 找到已完成的输出缓冲区
          outIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
          if (outIndex >= 0) {
            // 每120帧(两秒)检查一次是否旋转，防止未收到旋转信息
            decodeNum++
            if (decodeNum > 119) {
              decodeNum = 0
              ifRotation(decodec.getOutputFormat(outIndex))
            }
            decodec.releaseOutputBuffer(outIndex, true)
          } else if (outIndex == INFO_OUTPUT_FORMAT_CHANGED) {
            ifRotation(decodec.outputFormat)
          } else {
            Thread.sleep(8)
            continue
          }
        }
      } else {
        val track = configs.audioTrack
        while (configs.status == 1) {
          // 找到已完成的输出缓冲区
          outIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
          if (outIndex < 0) {
            Thread.sleep(4)
            continue
          }
          track.write(
            decodec.getOutputBuffer(outIndex)!!, bufferInfo.size, AudioTrack.WRITE_NON_BLOCKING
          )
          decodec.releaseOutputBuffer(outIndex, false)
        }
      }
    } catch (_: IllegalArgumentException) {
    } finally {
      if (mode == "video") {
        // 隐藏悬浮窗
        windowManager.removeView(configs.navView)
        windowManager.removeView(configs.surfaceView)
      }
      configs.status--
    }
  }

  // 控制报文输出
  private fun controlOutput() {
    try {
      while (configs.status == 0) Thread.sleep(20)
      while (configs.status == 1) {
        if (configs.controls.isEmpty()) {
          Thread.sleep(2)
          continue
        }
        try {
          val control = configs.controls.poll()
          configs.controlStream.write(control)
          configs.controlStream.flush()
        } catch (_: NullPointerException) {
        }
      }
    } finally {
      // 清空控制命令
      for (i in configs.controls) {
        configs.controlStream.write(configs.controls.poll())
      }
      configs.controlStream.flush()
      configs.controls.clear()
      configs.status--
    }
  }

  // 防止被控端熄屏
  private fun notOffScreen() {
    try {
      while (configs.status == 0) Thread.sleep(20)
      while (configs.status == 1) {
        configs.adbStream.write(" dumpsys deviceidle | grep mScreenOn \n")
        while (configs.status == 1) {
          val str = String(configs.adbStream.read())
          if (str.contains("mScreenOn=true")) break
          else if (str.contains("mScreenOn=false")) {
            configs.adbStream.write(" input keyevent 26 \n")
            break
          }
        }
        Thread.sleep(2000)
      }
    } finally {
      // 恢复分辨率
      configs.adbStream.write(" wm size reset \n")
      while (configs.status != -6) Thread.sleep(10)
      configs.adbStream.write(" ps -ef | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9 \n")
      configs.adbStream.close()
      // 取消常驻通知
      (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
      configs.status--
    }
  }

  // 被控端熄屏
  private fun setPowerOff() {
    val byteBuffer = ByteBuffer.allocate(2)
    byteBuffer.clear()
    byteBuffer.put(10)
    byteBuffer.put(0)
    byteBuffer.flip()
    configs.controls.offer(byteBuffer.array())
  }

  // 判断是否旋转
  private fun ifRotation(format: MediaFormat) {
    configs.remoteWidth = format.getInteger("width")
    configs.remoteHeight = format.getInteger("height")
    // 检测是否旋转
    if ((configs.remoteWidth > configs.remoteHeight && configs.localWidth < configs.localHeight) || (configs.remoteWidth < configs.remoteHeight && configs.localWidth > configs.localHeight)) {
      // surface
      var tmp = configs.localWidth
      configs.localWidth = configs.localHeight
      configs.localHeight = tmp
      configs.surfaceLayoutParams.apply {
        width = configs.localWidth
        height = configs.localHeight
      }
      // 导航球，旋转不改变位置
      if (configs.remoteWidth > configs.remoteHeight) {
        tmp = configs.navLayoutParams.y
        configs.navLayoutParams.y =
          configs.localHeight - configs.navLayoutParams.x - configs.navLayoutParams.width
        configs.navLayoutParams.x = tmp
      } else {
        tmp = configs.navLayoutParams.x
        configs.navLayoutParams.x =
          configs.localWidth - configs.navLayoutParams.y - configs.navLayoutParams.height
        configs.navLayoutParams.y = tmp
      }
      runOnUiThread {
        windowManager.updateViewLayout(configs.surfaceView, configs.surfaceLayoutParams)
        windowManager.updateViewLayout(configs.navView, configs.navLayoutParams)
      }
      requestedOrientation =
        if (configs.remoteWidth > configs.remoteHeight) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }

  // 从socket流中解析数据
  private fun readFrame(stream: DataInputStream): ByteArray {
    stream.readLong()
    val size = stream.readInt()
    val buffer = ByteArray(size)
    stream.readFully(buffer, 0, size)
    return buffer
  }

  // 设置通知栏
  @SuppressLint("LaunchActivityFromNotification")
  private fun setNotification() {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel("scrcpy_android", "chat", importance).apply {
        description = "常驻通知用于停止投屏"
      }
      notificationManager.createNotificationChannel(channel)
    }
    val intent = Intent("top.saymzx.notification")
    val builder = NotificationCompat.Builder(this, "scrcpy_android")
      .setSmallIcon(R.drawable.icon)
      .setContentTitle("投屏")
      .setContentText("点击关闭投屏")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setContentIntent(
        PendingIntent.getBroadcast(
          this,
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
      )
      .setAutoCancel(true)
      .setOngoing(true)
    notificationManager.notify(1, builder.build())
  }

  // 按键广播处理
  inner class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
      if (configs.status > 0) {
        // 恢复为未投屏状态
        configs.status = -1
        // 取消强制旋转
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      }
    }
  }
}