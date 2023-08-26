/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

import okio.Sink
import okio.buffer
import java.nio.ByteBuffer

class AdbWriter(sink: Sink) : AutoCloseable {

  private val bufferedSink = sink.buffer()

  fun writeConnect() = write(
    Constants.CMD_CNXN,
    Constants.CONNECT_VERSION,
    Constants.CONNECT_MAXDATA,
    Constants.CONNECT_PAYLOAD,
    0,
    Constants.CONNECT_PAYLOAD.size
  )

  fun writeAuth(authType: Int, authPayload: ByteArray) = write(
    Constants.CMD_AUTH,
    authType,
    0,
    authPayload,
    0,
    authPayload.size
  )

  fun writeOpen(localId: Int, destination: String) {
    val destinationBytes = destination.toByteArray()
    val buffer = ByteBuffer.allocate(destinationBytes.size + 1)
    buffer.put(destinationBytes)
    buffer.put(0)
    val payload = buffer.array()
    write(Constants.CMD_OPEN, localId, 0, payload, 0, payload.size)
  }

  fun writeWrite(localId: Int, remoteId: Int, payload: ByteArray, offset: Int, length: Int) {
    if (length < maxPayloadSize) {
      write(Constants.CMD_WRTE, localId, remoteId, payload, offset, length)
    } else {
      var tmpOffset = 0
      while (tmpOffset < payload.size) {
        val tmpLength = minOf(maxPayloadSize, length - tmpOffset)
        write(Constants.CMD_WRTE, localId, remoteId, payload, tmpOffset, tmpLength)
        tmpOffset += tmpLength
      }
    }
  }

  fun writeClose(localId: Int, remoteId: Int) {
    write(Constants.CMD_CLSE, localId, remoteId, null, 0, 0)
  }

  fun writeOkay(localId: Int, remoteId: Int) {
    write(Constants.CMD_OKAY, localId, remoteId, null, 0, 0)
  }


  var maxPayloadSize = 1024
  private fun write(
    command: Int,
    arg0: Int,
    arg1: Int,
    payload: ByteArray?,
    offset: Int,
    length: Int
  ) {
    synchronized(bufferedSink) {
      bufferedSink.apply {
        writeIntLe(command)
        writeIntLe(arg0)
        writeIntLe(arg1)
        if (payload == null) {
          writeIntLe(0)
          writeIntLe(0)
        } else {
          writeIntLe(length)
          writeIntLe(payloadChecksum(payload))
        }
        writeIntLe(command xor -0x1)
        if (payload != null) {
          write(payload, offset, length)
        }
        flush()
      }
    }
  }

  override fun close() {
    bufferedSink.close()
  }

  private fun payloadChecksum(payload: ByteArray): Int {
    var checksum = 0
    for (byte in payload) {
      checksum += byte.toUByte().toInt()
    }
    return checksum
  }
}