/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

import okio.Sink
import okio.buffer
import java.nio.ByteBuffer

class AdbWriter(sink: Sink) : AutoCloseable {

  private val bufferedSink = sink.buffer()

  fun writeConnect() = writeCommand(
    Constants.CMD_CNXN,
    Constants.CONNECT_VERSION,
    Constants.CONNECT_MAXDATA,
    Constants.CONNECT_PAYLOAD,
    0,
    Constants.CONNECT_PAYLOAD.size
  )

  fun writeAuth(authType: Int, authPayload: ByteArray) = writeCommand(
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
    writeCommand(Constants.CMD_OPEN, localId, 0, payload)
  }

  fun writeWrite(localId: Int, remoteId: Int, payload: ByteArray, offset: Int, length: Int) {
    if (length < maxPayloadSize) {
      writeCommand(Constants.CMD_WRTE, localId, remoteId, payload, offset, length)
    } else {
      var tmpOffset = offset
      while (tmpOffset < offset + length) {
        val tmpLength = minOf(maxPayloadSize, offset + length - tmpOffset)
        writeCommand(Constants.CMD_WRTE, localId, remoteId, payload, tmpOffset, tmpLength)
        tmpOffset += tmpLength
      }
    }
  }

  fun writeClose(localId: Int, remoteId: Int) {
    writeCommand(Constants.CMD_CLSE, localId, remoteId)
  }

  fun writeOkay(localId: Int, remoteId: Int) {
    writeCommand(Constants.CMD_OKAY, localId, remoteId)
  }


  var maxPayloadSize = 1024
  private inline fun writeCommand(
    command: Int,
    arg0: Int,
    arg1: Int,
    payload: ByteArray? = null,
    payloadOffset: Int = 0,
    payloadLength: Int = 0
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
          writeIntLe(payloadLength)
          writeIntLe(payloadChecksum(payload, payloadOffset, payloadLength))
        }
        writeIntLe(command xor -0x1)
        payload?.let { write(it, payloadOffset, payloadLength) }
        flush()
      }
    }
  }

  override fun close() {
    bufferedSink.close()
  }

  private fun payloadChecksum(payload: ByteArray, offset: Int, length: Int): Int {
    var checksum = 0
    for (i in offset until offset + length) {
      checksum += payload[i].toUByte().toInt()
    }
    return checksum
  }
}