/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

import okhttp3.internal.wait
import okio.Buffer

class AdbStream(
  private val localId: Int,
  private val adbWriter: AdbWriter,
  val isNeedSource: Boolean
) {
  var remoteId = 0

  // -1为已关闭，0为连接中，1为等待恢复，2为可读写
  var status = 0

  // 可写
  val canWrite = Object()

  val source = Buffer()

  fun write(byteArray: ByteArray) {
    if (status == -1) return
    else if (status != 2) synchronized(canWrite) { canWrite.wait() }
    status = 1
    adbWriter.writeWrite(localId, remoteId, byteArray, 0, byteArray.size)
  }

  fun close() {
    status = -1
    adbWriter.writeClose(localId, remoteId)
  }

  fun readInt(): Int {
    require(4)
    return source.readInt()
  }

  fun readByte(): Byte {
    require(1)
    return source.readByte()
  }

  fun readByteArray(size: Int): ByteArray {
    require(size.toLong())
    return source.readByteArray(size.toLong())
  }

  fun readShort(): Short {
    require(2)
    return source.readShort()
  }

  fun readLong(): Long {
    require(8)
    return source.readLong()
  }

  private fun require(byteCount: Long) {
    while (true) {
      if (!source.request(byteCount)) synchronized(source) {
        source.wait()
      } else break
    }
  }
}
