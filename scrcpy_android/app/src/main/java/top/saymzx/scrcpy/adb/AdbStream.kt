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

import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer

class AdbStream(
  private val localId: Int,
  private val maxPayloadSize: Int,
  private val adbWriter: AdbWriter
) {

  var remoteId = 0

  private var sourceBuffer = Buffer()

  fun push(byteArray: ByteArray) {
    sourceBuffer.write(byteArray)
  }

  val source = object : Source {

    override fun read(sink: Buffer, byteCount: Long): Long {
      val tmp = ByteArray(byteCount.toInt())
      val len = sourceBuffer.read(tmp, 0, byteCount.toInt())
      sink.write(tmp, 0, maxOf(len, 0))
      return len.toLong()
    }

    override fun close() {}

    override fun timeout() = Timeout.NONE
  }.buffer()

  val sink = object : Sink {

    override fun write(source: Buffer, byteCount: Long) {
      adbWriter.writeWrite()
      sinkBuffer.write(source, byteCount)
    }

    override fun close() {}

    override fun flush() {}

    override fun timeout() = Timeout.NONE
  }.buffer()
}
