/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

import okio.Source
import okio.buffer

internal class AdbReader(source: Source) : AutoCloseable {

  private val bufferedSource = source.buffer()

  fun readMessage(): AdbMessage {
    synchronized(bufferedSource) {
      bufferedSource.apply {
        val command = readIntLe()
        val arg0 = readIntLe()
        val arg1 = readIntLe()
        val payloadLength = readIntLe()
        val checksum = readIntLe()
        val magic = readIntLe()
        val payload = readByteArray(payloadLength.toLong())
        return AdbMessage(command, arg0, arg1, payloadLength, payload)
      }
    }
  }

  override fun close() {
    bufferedSource.close()
  }
}
