/*
 * Copyright (c) 2021 mobile.dev inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package top.saymzx.scrcpy.adb

import okhttp3.internal.notify
import okhttp3.internal.wait
import okio.Buffer

class AdbStream(
  private val localId: Int,
  private val adbWriter: AdbWriter,
  private val isNeedSource: Boolean
) {
  var remoteId = 0

  // -1为已关闭，0为连接中，1为等待恢复，2为可读写
  var status = 0

  // 可写
  val canWrite = Object()

  val source = Buffer()

  fun pushToSource(byteArray: ByteArray) {
    if (isNeedSource) {
      source.write(byteArray)
      synchronized(source) {
        source.notify()
      }
    }
  }

  fun write(byteArray: ByteArray) {
    if (status == -1) return
    else if (status != 2) synchronized(canWrite) {
      canWrite.wait()
    }
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
