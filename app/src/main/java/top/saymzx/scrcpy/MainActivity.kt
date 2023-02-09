package top.saymzx.scrcpy

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.xml.bind.DatatypeConverter
import kotlin.math.abs


class MainActivity : Activity() {

  //  可配置项：
  // 视频比特率
  private val videoBitrate = 16000000

  // 最大帧率
  private val maxFps = 40

  // 被控端IP地址
  private val remoteIp = "192.168.3.100"

  // 被控端ADB端口
  private val remotePort = 5555

  // 被控端socket端口
  private val remoteSocketPort = 5005

  // 被控端屏幕大小（配置无需谨慎，后期会自动更正，仅取长宽最大值限制分辨率用）
  private var remoteWidth = 720
  private var remoteHeight = 1600

  // 不可配置项：
  // 主控端屏幕大小(不需要调整，会自动获取)
  private var localWidth = 1080
  private var localHeight = 2400

  // 解码器
  private lateinit var decodec: MediaCodec

  // 状态标识(0为初始，1为推送并启动完server，2为投屏中)
  private var status = 0

  // 是否横屏
  private var isLandscapeOld = false
  private var isLandscape = false

  // 视频流
  private lateinit var videoStream: DataInputStream
  private lateinit var controlStream: DataOutputStream

  // 视频流配置信息
  private lateinit var spsBuffer: ByteBuffer
  private lateinit var ppsBuffer: ByteBuffer

  // 触控输入队列
  private val controls = ArrayDeque<ByteBuffer>()

  // 显示的控件
  private lateinit var surfaceView: SurfaceView
  private lateinit var surface: Surface
  private lateinit var windowManager: WindowManager
  private lateinit var layoutParam: LayoutParams

  // 触摸xy记录（为了适配有些设备过于敏感，将点击识别为小范围移动）
  private val pointerList = ArrayList<IntArray>()

  // 创建界面
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // 初始化
    init()

    // 发送并启动scrcpy server.jar
    Thread { sendServer() }.start()

    // 启动scrcpy client并连接server
    Thread { startClient() }.start()

    // 启动控制报文发送线程
    Thread { controlHandle() }.start()

  }

  // 初始化配置
  private fun init() {
    // 强制竖屏
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    // 获取主控端分辨率
    val metric = DisplayMetrics()
    getWindowManager().defaultDisplay.getRealMetrics(metric)
    localWidth = metric.widthPixels
    localHeight = metric.heightPixels

    // 创建显示用的surface
    surfaceView = SurfaceView(this)
    surface = surfaceView.holder.surface

    // 创建显示悬浮窗
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    layoutParam = LayoutParams().apply {
      type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        LayoutParams.TYPE_SYSTEM_ALERT
      }
      flags =
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_KEEP_SCREEN_ON
      //位置大小设置
      width = localWidth
      height = localHeight
      gravity = Gravity.START or Gravity.TOP
      x = 0
      y = 0
    }
    surfaceView.setOnTouchListener { _, event -> surfaceOnTouchEvent(event) }
    // 将悬浮窗控件添加到WindowManager
    windowManager.addView(surfaceView, layoutParam)

    // 监控熄屏（用于退出程序）
    val filter = IntentFilter()
    filter.addAction(Intent.ACTION_SCREEN_OFF)
    class BatInfoReceiver : BroadcastReceiver() {
      override fun onReceive(p0: Context?, p1: Intent?) {
        finishAndRemoveTask()
        Runtime.getRuntime().exit(0)
      }
    }

    val screenReceiver = BatInfoReceiver()
    registerReceiver(screenReceiver, filter)

  }

  // 触摸事件
  private fun surfaceOnTouchEvent(event: MotionEvent?): Boolean {
    // 检查是否已连接
    if (status != 2) return true
    // 触摸类型
    when (event!!.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        val i = event.actionIndex
        val p = event.getPointerId(i)
        var x = event.getX(i).toInt()
        var y = event.getY(i).toInt()
        if (isLandscape) {
          val tmpX = x
          x = ((y.toFloat() / localWidth) * remoteWidth).toInt()
          y = (((localHeight - tmpX).toFloat() / localHeight) * remoteHeight).toInt()
        } else {
          x = ((x.toFloat() / localWidth) * remoteWidth).toInt()
          y = ((y.toFloat() / localHeight) * remoteHeight).toInt()
        }
        createTouchPacket(0, p, x, y)
        // 记录按下xy信息
        val xy = try {
          pointerList[p]
        } catch (_: IndexOutOfBoundsException) {
          pointerList.add(p, IntArray(2))
          pointerList[p]
        }
        xy[0] = x
        xy[1] = y
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
        val i = event.actionIndex
        val p = event.getPointerId(i)
        var x = event.getX(i).toInt()
        var y = event.getY(i).toInt()
        if (isLandscape) {
          val tmpX = x
          x = ((y.toFloat() / localWidth) * remoteWidth).toInt()
          y = (((localHeight - tmpX).toFloat() / localHeight) * remoteHeight).toInt()
        } else {
          x = ((x.toFloat() / localWidth) * remoteWidth).toInt()
          y = ((y.toFloat() / localHeight) * remoteHeight).toInt()
        }
        createTouchPacket(1, p, x, y)
      }
      MotionEvent.ACTION_MOVE -> {
        // 移动为了效率，一个event中会存放多个移动事件
        val pointerCount: Int = event.pointerCount
        val historySize: Int = event.historySize
        for (h in 0 until historySize) {
          for (p in 0 until pointerCount) {
            var x = event.getHistoricalX(p, h).toInt()
            var y = event.getHistoricalY(p, h).toInt()
            if (isLandscape) {
              val tmpX = x
              x = ((y.toFloat() / localWidth) * remoteWidth).toInt()
              y = (((localHeight - tmpX).toFloat() / localHeight) * remoteHeight).toInt()
            } else {
              x = ((x.toFloat() / localWidth) * remoteWidth).toInt()
              y = ((y.toFloat() / localHeight) * remoteHeight).toInt()
            }
            val xy = pointerList[p]
            // 为了适配有些设备过于敏感，将点击识别为小范围移动，并排除系统返回桌面手势
            if ((abs(xy[0] - x) > 6 && abs(xy[1] - y) > 6) || y < 10) {
              createTouchPacket(2, p, x, y)
            }
          }
        }
        for (p in 0 until pointerCount) {
          var x = event.getX(p).toInt()
          var y = event.getY(p).toInt()
          if (isLandscape) {
            val tmpX = x
            x = ((y.toFloat() / localWidth) * remoteWidth).toInt()
            y = (((localHeight - tmpX).toFloat() / localHeight) * remoteHeight).toInt()
          } else {
            x = ((x.toFloat() / localWidth) * remoteWidth).toInt()
            y = ((y.toFloat() / localHeight) * remoteHeight).toInt()
          }
          val xy = pointerList[p]
          if ((abs(xy[0] - x) > 6 && abs(xy[1] - y) > 6) || y < 10) createTouchPacket(2, p, x, y)
        }
      }
    }
    return true
  }

  // 创建触摸控制包
  private fun createTouchPacket(action: Byte, pointerId: Int, x: Int, y: Int) {
    val byteBuffer = ByteBuffer.allocate(28)
    byteBuffer.clear()
    // 触摸事件
    byteBuffer.put(2)
    // 触摸类型
    byteBuffer.put(action)
    // pointerId
    byteBuffer.putLong(pointerId.toLong())
    // 坐标位置
    byteBuffer.putInt(x)
    byteBuffer.putInt(y)
    // 屏幕尺寸
    byteBuffer.putShort(remoteWidth.toShort())
    byteBuffer.putShort(remoteHeight.toShort())
    // 按压力度presureInt和buttons
    byteBuffer.putShort(0)
    byteBuffer.putInt(0)
    byteBuffer.flip()
    controls.addLast(byteBuffer)
  }

  // 处理控制事件
  private fun controlHandle() {
    var byteBuffer: ByteBuffer?
    while (true) {
      try {
        byteBuffer = controls.removeFirst()
        val array = ByteArray(byteBuffer.remaining())
        byteBuffer.get(array)
        controlStream.write(array)
      } catch (_: NoSuchElementException) {
        // 无触摸时减少扫描频率，降低性能消耗
        Thread.sleep(4)
      }
    }
  }

  // 发送并启动scrcpy server.jar
  private fun sendServer() {
    // 读取scrcpy server
    val assetManager = assets
    val serverFileBase64: ByteArray?
    val inputStream = assetManager.open("scrcpy-server.jar")
    val buffer = ByteArray(inputStream.available())
    inputStream.read(buffer)
    serverFileBase64 = Base64.encode(buffer, 2)
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
    val socket = Socket(remoteIp, remotePort)
    val connection = AdbConnection.create(socket, crypto)
    connection.connect()
    // 发送文件
    val stream = connection.open("shell:")
    stream.write(" cd /data/local/tmp " + '\n')
    stream.write(" rm serverBase64 " + '\n')
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
      stream.write(" echo $serverBase64part >> serverBase64\n")
      Thread.sleep(50)
    }
    stream.write(" base64 -d < serverBase64 > scrcpy-server.jar && rm serverBase64" + '\n')
    Thread.sleep(100)
    // 启动scrcpy server
    var command = "ps aux | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9"
    stream.write(command + '\n')
    val maxSize = remoteHeight.coerceAtLeast(remoteWidth)
    command =
      " CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 1.25 log_level=error bit_rate=$videoBitrate max_size=$maxSize max_fps=$maxFps tunnel_forward=true stay_awake=true power_off_on_close=false clipboard_autosync=false codec-options=profile=1,level=1 "
    stream.write(command + '\n')
    // 修改状态
    status = 1
  }

  // 启动scrcpy client
  private fun startClient() {
    // 等待推送并启动server
    while (true) {
      if (status == 1) break
      Thread.sleep(200)
    }
    // 等待连接server
    var videSocket: Socket? = null
    var controlSocket: Socket? = null
    for (i in 1..5) {
      Thread.sleep(200)
      try {
        videSocket = Socket(remoteIp, remoteSocketPort)
        break
      } catch (e: IOException) {
        Log.e("错误", "连接视频流失败")
      }
      Thread.sleep(150)
    }
    for (i in 1..5) {
      try {
        controlSocket = Socket(remoteIp, remoteSocketPort)
        break
      } catch (e: IOException) {
        Log.e("错误", "连接控制流失败")
      }
      Thread.sleep(200)
    }
    videoStream = DataInputStream(videSocket!!.getInputStream())
    controlStream = DataOutputStream(controlSocket!!.getOutputStream())
    // 获取被控端屏幕大小:起始报文：0（一个字节）+设备名（64字节）+屏幕宽（2字节）+屏幕高（2字节）
    val tmpbuffer1 = ByteArray(65)
    videoStream.readFully(tmpbuffer1, 0, 65)
    remoteWidth = videoStream.readShort().toInt()
    remoteHeight = videoStream.readShort().toInt()
    // 检测是否横屏
    if ((remoteWidth > remoteHeight)) {
      val temp: Int = localHeight
      localHeight = localWidth
      localWidth = temp
      isLandscape = true
      isLandscapeOld = true
    }
    // 初始化解码器
    readPacket()
    setDecodec()
    // 开始循环接受画面帧并解码显示，传输控制事件
    val bufferInfo = BufferInfo()
    var inIndex: Int
    // 修改状态
    status = 2
    // 熄屏控制
    setPowerOff()
    // 开始解码
    while (true) {
      // 找到已完成的输出缓冲区
      inIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
      // 清空已完成的缓冲区
      while (inIndex >= 0) {
        val newFomat = decodec.getOutputFormat(inIndex)
        remoteWidth = newFomat.getInteger("width")
        remoteHeight = newFomat.getInteger("height")
        // 检测是否旋转
        if ((remoteWidth > remoteHeight) && localWidth < localHeight) {
          val temp: Int = localHeight
          localHeight = localWidth
          localWidth = temp
          isLandscape = true
        } else if ((remoteWidth < remoteHeight) && localWidth > localHeight) {
          val temp: Int = localHeight
          localHeight = localWidth
          localWidth = temp
          isLandscape = false
        }
        decodec.releaseOutputBuffer(inIndex, true)
        inIndex = decodec.dequeueOutputBuffer(bufferInfo, 0)
      }
      // 检测是否发生了旋转
      if (isLandscapeOld != isLandscape) {
        isLandscapeOld = isLandscape
        val format = MediaFormat.createVideoFormat("video/avc", remoteWidth, remoteHeight)
        format.setByteBuffer("csd-0", spsBuffer)
        format.setByteBuffer("csd-1", ppsBuffer)
        if (isLandscape) format.setInteger(MediaFormat.KEY_ROTATION, 90)
        decodec.reset()
        decodec.configure(format, surface, null, 0)
        decodec.start()
      }
      // 找到一个空的输入缓冲区
      inIndex = decodec.dequeueInputBuffer(-1)
      // 向缓冲区输入数据帧
      val buffer = readPacket()
      decodec.getInputBuffer(inIndex)!!.put(buffer)
      // 提交解码器解码
      decodec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
    }
  }

  // 从socket流中解析数据
  private fun readPacket(): ByteArray {
    val size = videoStream.readInt()
    val type = videoStream.readInt()
    val buffer = ByteArray(size)
    videoStream.readFully(buffer, 0, size)
    // 关键帧，第一帧解析给解码器初始化
    if (type == 0) getStreamSettings(buffer)
    return buffer
  }

  // 准备解码器
  private fun setDecodec() {
    // 创建解码器（使用h264格式，兼容性广，并且比h265等编码更快）
    decodec = MediaCodec.createDecoderByType("video/avc")
    // 配置输出画面至软件界面
    val format = MediaFormat.createVideoFormat("video/avc", remoteWidth, remoteHeight)
    format.setByteBuffer("csd-0", spsBuffer)
    format.setByteBuffer("csd-1", ppsBuffer)
    if (isLandscape) format.setInteger(MediaFormat.KEY_ROTATION, 90)
    decodec.configure(format, surface, null, 0)
    // 启动解码器
    decodec.start()
  }

  // 解析关键帧
  private fun getStreamSettings(buffer: ByteArray) {
    val sps: ByteArray
    val pps: ByteArray
    val spsPpsBuffer = ByteBuffer.wrap(buffer)
    while (spsPpsBuffer.get().toInt() == 0 && spsPpsBuffer.get().toInt() == 0 && spsPpsBuffer.get()
        .toInt() == 0 && spsPpsBuffer.get().toInt() != 1
    ) {
    }
    var ppsIndex: Int = spsPpsBuffer.position()
    sps = ByteArray(ppsIndex - 4)
    System.arraycopy(buffer, 0, sps, 0, sps.size)
    ppsIndex -= 4
    pps = ByteArray(buffer.size - ppsIndex)
    System.arraycopy(buffer, ppsIndex, pps, 0, pps.size)
    // sps
    spsBuffer = ByteBuffer.wrap(sps, 0, sps.size)
    // pps
    ppsBuffer = ByteBuffer.wrap(pps, 0, pps.size)
  }

  // 熄屏控制
  private fun setPowerOff() {
    val byteBuffer = ByteBuffer.allocate(2)
    byteBuffer.clear()
    byteBuffer.put(10)
    byteBuffer.put(0)
    byteBuffer.flip()
    controls.addLast(byteBuffer)
  }

}