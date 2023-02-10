package top.saymzx.scrcpy

import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModel
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.bind.DatatypeConverter
import kotlin.math.abs

class Configs : ViewModel() {
  //  可配置项：
  // 视频比特率
  val videoBitrate = 16000000

  // 最大帧率
  val maxFps = 60

  // 被控端IP地址
  val remoteIp = "192.168.3.201"

  // 被控端ADB端口
  val remotePort = 5555

  // 被控端socket端口
  val remoteSocketPort = 5005

  // 被控端屏幕大小（配置无需谨慎，后期会自动更正，仅取长宽最大值限制分辨率用）
  var remoteWidth = 720
  var remoteHeight = 1600

  // 不可配置项：
  // 主控端屏幕大小(不需要调整，会自动获取)
  var localWidth = 1080
  var localHeight = 2400

  // 解码器
  lateinit var decodec: MediaCodec

  // 状态标识(0为初始，1为推送并启动完server，2为投屏中)
  var status = 0

  // 视频流
  lateinit var videoStream: DataInputStream
  lateinit var controlStream: DataOutputStream

  // 悬浮窗
  lateinit var floatLayoutParams: LayoutParams

  @SuppressLint("StaticFieldLeak")
  lateinit var surfaceView: SurfaceView

  // 触控输入队列
  val controls = LinkedList<ByteBuffer>() as Queue<ByteBuffer>

  // 触摸xy记录（为了适配有些设备过于敏感，将点击识别为小范围移动）
  val pointerList = ArrayList<IntArray>()

  // 屏幕广播（熄屏与旋转）
  var screenReceiver: MainActivity.ScreenReceiver? = null
}

class MainActivity : Activity() {

  private val configs = Configs()

  // 创建界面
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // 注册广播用以关闭程序
    val filter = IntentFilter()
    filter.addAction(Intent.ACTION_SCREEN_OFF)
    filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
    configs.screenReceiver = ScreenReceiver()
    registerReceiver(configs.screenReceiver, filter)

    if (configs.status == 0) {
      // 软件初始化
      val surface = init()

      // ADB发送并启动scrcpy server.jar
      Thread { sendServer() }.start()

      // 启动scrcpy client,并完成连接初始化等操作
      Thread { startClient(surface) }.start()

      // 视频流输入解码
      Thread { videoHandle() }.start()

      // 解码输出
      Thread { decodecHandle() }.start()

      // 控制流输出
      Thread { controlHandle() }.start()
    }
  }

  // 初始化配置
  @SuppressLint("ClickableViewAccessibility")
  private fun init(): Surface {

    // 获取主控端分辨率
    val metric = DisplayMetrics()
    windowManager.defaultDisplay.getRealMetrics(metric)
    configs.localWidth = metric.widthPixels
    configs.localHeight = metric.heightPixels

    // 创建显示悬浮窗
    configs.surfaceView = SurfaceView(this)
    val surface = configs.surfaceView.holder.surface
    configs.floatLayoutParams = LayoutParams().apply {
      type =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LayoutParams.TYPE_APPLICATION_OVERLAY
        else LayoutParams.TYPE_SYSTEM_ALERT
      flags =
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_KEEP_SCREEN_ON
      //位置大小设置
      width = configs.localWidth
      height = configs.localHeight
      gravity = Gravity.START or Gravity.TOP
      x = 0
      y = 0
    }
    // 将悬浮窗控件添加到WindowManager
    windowManager.addView(configs.surfaceView, configs.floatLayoutParams)

    // 监控触控操作
    configs.surfaceView.setOnTouchListener { _, event -> surfaceOnTouchEvent(event) }

    return surface
  }

  // 触摸事件
  private fun surfaceOnTouchEvent(event: MotionEvent?): Boolean {
    // 未连接状态不处理
    if (configs.status != 2) return true
    // 获取本次动作的手指ID以及xy坐标
    val i = event!!.actionIndex
    val p = event.getPointerId(i)
    var x = ((event.getX(i) / configs.localWidth) * configs.remoteWidth).toInt()
    var y = ((event.getY(i) / configs.localHeight) * configs.remoteHeight).toInt()
    // 防止旋转过程中可能出现的计算除负值情况
    if (x < 0 || y < 0) {
      x = 0
      y = 0
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        createTouchPacket(event.actionMasked.toByte(), p, x, y)
        // 记录按下xy信息
        val xy = try {
          configs.pointerList[p]
        } catch (_: IndexOutOfBoundsException) {
          configs.pointerList.add(p, IntArray(2))
          configs.pointerList[p]
        }
        xy[0] = x
        xy[1] = y
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
        createTouchPacket(event.actionMasked.toByte(), p, x, y)
      }
      MotionEvent.ACTION_MOVE -> {
        try {
          val xy = configs.pointerList[p]
          // 适配一些机器将点击视作小范围移动
          if ((abs(xy[0] - x) > 4 && abs(xy[1] - y) > 4) || y < 6) createTouchPacket(2, p, x, y)
        } catch (_: IndexOutOfBoundsException) {
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
    byteBuffer.putShort(configs.remoteWidth.toShort())
    byteBuffer.putShort(configs.remoteHeight.toShort())
    // 按压力度presureInt和buttons
    byteBuffer.putShort(0)
    byteBuffer.putInt(0)
    byteBuffer.flip()
    configs.controls.offer(byteBuffer)
  }

  // 处理控制事件
  private fun controlHandle() {
    // 等待client初始化完成
    while (configs.status != 2) {
      Thread.sleep(400)
    }
    var byteBuffer: ByteBuffer?
    while (true) {
      byteBuffer = configs.controls.poll()
      if (byteBuffer == null) {
        Thread.sleep(2)
        continue
      }
      val array = ByteArray(byteBuffer.remaining())
      byteBuffer.get(array)
      configs.controlStream.write(array)
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
    val socket = Socket(configs.remoteIp, configs.remotePort)
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
    }
    stream.write(" base64 -d < serverBase64 > scrcpy-server.jar && rm serverBase64" + '\n')
    Thread.sleep(100)
    // 启动scrcpy server
    var command = "ps aux | grep scrcpy | grep -v grep | awk '{print $2}' | xargs kill -9"
    stream.write(command + '\n')
    val maxSize = configs.remoteHeight.coerceAtLeast(configs.remoteWidth)
    command =
      " CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 1.25 log_level=error bit_rate=${configs.videoBitrate} max_size=$maxSize max_fps=${configs.maxFps} tunnel_forward=true stay_awake=true power_off_on_close=true clipboard_autosync=false codec-options=profile=1,level=1 "
    stream.write(command + '\n')
    // 修改状态
    configs.status = 1
  }

  // 启动scrcpy client
  private fun startClient(surface: Surface) {
    // 等待发送server完成
    while (configs.status != 1) {
      Thread.sleep(400)
    }
    // 连接server
    var videSocket: Socket? = null
    var controlSocket: Socket? = null
    for (i in 1..5) {
      try {
        if (videSocket == null) videSocket = Socket(configs.remoteIp, configs.remoteSocketPort)
        controlSocket = Socket(configs.remoteIp, configs.remoteSocketPort)
        break
      } catch (e: IOException) {
        Log.e("错误", "连接server失败，将再次尝试")
        Thread.sleep(800)
      }
    }
    // 在这如果还没连上会崩溃退出
    configs.videoStream = DataInputStream(videSocket!!.getInputStream())
    configs.controlStream = DataOutputStream(controlSocket!!.getOutputStream())
    // 获取被控端屏幕大小:起始报文：0（一个字节）+设备名（64字节）+屏幕宽（2字节）+屏幕高（2字节）
    val tmpbuffer1 = ByteArray(65)
    configs.videoStream.readFully(tmpbuffer1, 0, 65)
    configs.remoteWidth = configs.videoStream.readShort().toInt()
    configs.remoteHeight = configs.videoStream.readShort().toInt()
    // 初始化解码器
    setDecodec(surface)
    // 修改状态
    configs.status = 2
    // 熄屏控制
    setPowerOff()
    // 检测是否横屏
    if ((configs.remoteWidth > configs.remoteHeight)) {
      val temp: Int = configs.localHeight
      configs.localHeight = configs.localWidth
      configs.localWidth = temp
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
  }

  // 视频流输入解码
  private fun videoHandle() {
    // 等待client初始化完成
    while (configs.status != 2) {
      Thread.sleep(400)
    }
    var inIndex: Int
    // 开始解码
    while (true) {
      // 找到一个空的输入缓冲区
      inIndex = configs.decodec.dequeueInputBuffer(-1)
      // 向缓冲区输入数据帧
      val buffer = readPacket()
      configs.decodec.getInputBuffer(inIndex)!!.put(buffer)
      // 提交解码器解码
      configs.decodec.queueInputBuffer(inIndex, 0, buffer.size, 0, 0)
    }
  }

  // 解析数据到界面
  private fun decodecHandle() {
    // 等待client初始化完成
    while (configs.status != 2) {
      Thread.sleep(400)
    }
    var inIndex: Int
    val bufferInfo = BufferInfo()
    while (true) {
      // 找到已完成的输出缓冲区
      inIndex = configs.decodec.dequeueOutputBuffer(bufferInfo, 100)
      if (inIndex < 0) continue
      // 清空已完成的缓冲区
      val fomat = configs.decodec.getOutputFormat(inIndex)
      configs.remoteWidth = fomat.getInteger("width")
      configs.remoteHeight = fomat.getInteger("height")
      // 检测是否旋转
      if ((configs.remoteWidth > configs.remoteHeight) && configs.localWidth < configs.localHeight) {
        val temp: Int = configs.localHeight
        configs.localHeight = configs.localWidth
        configs.localWidth = temp
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      } else if ((configs.remoteWidth < configs.remoteHeight) && configs.localWidth > configs.localHeight) {
        val temp: Int = configs.localHeight
        configs.localHeight = configs.localWidth
        configs.localWidth = temp
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      }
      configs.decodec.releaseOutputBuffer(inIndex, true)
    }
  }

  // 从socket流中解析数据
  private fun readPacket(): ByteArray {
    val size = configs.videoStream.readInt()
    val buffer = ByteArray(size)
    configs.videoStream.readFully(buffer, 0, size)
    return buffer
  }

  // 准备解码器
  private fun setDecodec(surface: Surface) {
    // 创建解码器（使用h264格式，兼容性广，并且比h265等编码更快）
    configs.decodec = MediaCodec.createDecoderByType("video/avc")
    // 配置输出画面至软件界面
    val format =
      MediaFormat.createVideoFormat("video/avc", configs.remoteWidth, configs.remoteHeight)
    // 获取视频编码信息sps和pps
    val spsApps = getStreamSettings(readPacket())
    format.setByteBuffer("csd-0", spsApps[0])
    format.setByteBuffer("csd-1", spsApps[1])
    configs.decodec.configure(format, surface, null, 0)
    // 启动解码器
    configs.decodec.start()
  }

  // 解析关键帧
  private fun getStreamSettings(buffer: ByteArray): ArrayList<ByteBuffer> {
    val sps: ByteArray
    val pps: ByteArray
    val spsAppsBuffer = ByteBuffer.wrap(buffer)
    while (spsAppsBuffer.get().toInt() == 0 && spsAppsBuffer.get()
        .toInt() == 0 && spsAppsBuffer.get().toInt() == 0 && spsAppsBuffer.get().toInt() != 1
    ) {
    }
    var ppsIndex: Int = spsAppsBuffer.position()
    sps = ByteArray(ppsIndex - 4)
    System.arraycopy(buffer, 0, sps, 0, sps.size)
    ppsIndex -= 4
    pps = ByteArray(buffer.size - ppsIndex)
    System.arraycopy(buffer, ppsIndex, pps, 0, pps.size)

    val spsApps = ArrayList<ByteBuffer>()
    spsApps.add(ByteBuffer.wrap(sps, 0, sps.size))
    spsApps.add(ByteBuffer.wrap(pps, 0, pps.size))
    return spsApps
  }

  // 被控端熄屏
  private fun setPowerOff() {
    val byteBuffer = ByteBuffer.allocate(2)
    byteBuffer.clear()
    byteBuffer.put(10)
    byteBuffer.put(0)
    byteBuffer.flip()
    configs.controls.offer(byteBuffer)
  }

  // 屏幕广播处理
  inner class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {

      when (p1?.action) {
        Intent.ACTION_CONFIGURATION_CHANGED -> {
          unregisterReceiver(configs.screenReceiver)
          configs.floatLayoutParams.apply {
            width = configs.localWidth
            height = configs.localHeight
          }
          windowManager.updateViewLayout(configs.surfaceView, configs.floatLayoutParams)
        }
        Intent.ACTION_SCREEN_OFF -> {
          unregisterReceiver(configs.screenReceiver)
          finishAndRemoveTask()
          Runtime.getRuntime().exit(0)
        }
      }
    }
  }

}