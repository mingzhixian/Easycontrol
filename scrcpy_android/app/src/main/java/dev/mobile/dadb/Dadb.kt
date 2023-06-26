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

package dev.mobile.dadb

import android.os.Build
import androidx.annotation.RequiresApi
import dev.mobile.dadb.adbserver.AdbServer
import okio.*
import java.io.File
import java.io.IOException
import java.nio.file.Files

interface Dadb : AutoCloseable {

  @Throws(IOException::class)
  fun open(destination: String): AdbStream

  fun supportsFeature(feature: String): Boolean

  @Throws(IOException::class)
  fun shell(command: String): AdbShellResponse {
    openShell(command).use { stream ->
      return stream.readAll()
    }
  }

  @Throws(IOException::class)
  fun openShell(command: String = ""): AdbShellStream {
    val stream = open("shell,v2,raw:$command")
    return AdbShellStream(stream)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  @Throws(IOException::class)
  fun push(
    src: File,
    remotePath: String,
    mode: Int = readMode(src),
    lastModifiedMs: Long = src.lastModified()
  ) {
    push(src.source(), remotePath, mode, lastModifiedMs)
  }

  @Throws(IOException::class)
  fun push(source: Source, remotePath: String, mode: Int, lastModifiedMs: Long) {
    openSync().use { stream ->
      stream.send(source, remotePath, mode, lastModifiedMs)
    }
  }

  @Throws(IOException::class)
  fun pull(dst: File, remotePath: String) {
    pull(dst.sink(append = false), remotePath)
  }

  @Throws(IOException::class)
  fun pull(sink: Sink, remotePath: String) {
    openSync().use { stream ->
      stream.recv(sink, remotePath)
    }
  }

  @Throws(IOException::class)
  fun openSync(): AdbSyncStream {
    val stream = open("sync:")
    return AdbSyncStream(stream)
  }

  @Throws(IOException::class)
  fun abbExec(vararg command: String): AdbStream {
    if (!supportsFeature("abb_exec")) throw UnsupportedOperationException("abb_exec is not supported on this version of Android")
    val destination = "abb_exec:${command.joinToString("\u0000")}"
    return open(destination)
  }

  companion object {

    private const val MIN_EMULATOR_PORT = 5555
    private const val MAX_EMULATOR_PORT = 5683

    @JvmStatic
    @JvmOverloads
    fun create(host: String, port: Int, keyPair: AdbKeyPair? = AdbKeyPair.readDefault()): Dadb =
      DadbImpl(host, port, keyPair)

    @JvmStatic
    @JvmOverloads
    fun discover(
      host: String = "localhost",
      keyPair: AdbKeyPair? = AdbKeyPair.readDefault()
    ): Dadb? {
      return list(host, keyPair).firstOrNull()
    }

    @JvmStatic
    @JvmOverloads
    fun list(
      host: String = "localhost",
      keyPair: AdbKeyPair? = AdbKeyPair.readDefault()
    ): List<Dadb> {
      val dadbs = AdbServer.listDadbs(adbServerHost = host)
      if (dadbs.isNotEmpty()) return dadbs

      return (MIN_EMULATOR_PORT..MAX_EMULATOR_PORT).mapNotNull { port ->
        val dadb = create(host, port, keyPair)
        val response = try {
          dadb.shell("echo success").allOutput
        } catch (ignore: Throwable) {
          null
        }
        if (response == "success\n") {
          dadb
        } else {
          null
        }
      }
    }

    private fun waitRootOrClose(dadb: Dadb, root: Boolean) {
      while (true) {
        try {
          val response = dadb.shell("getprop service.adb.root")
          val propValue = if (root) 1 else 0
          if (response.output == "$propValue\n") return
        } catch (e: IOException) {
          return
        }
      }
    }

    private fun restartAdb(dadb: Dadb, destination: String): String {
      dadb.open(destination).use { stream ->
        return stream.source.readUntil('\n'.code.toByte()).readString(Charsets.UTF_8)
      }
    }

    private fun BufferedSource.readUntil(endByte: Byte): Buffer {
      val buffer = Buffer()
      while (true) {
        val b = readByte()
        buffer.writeByte(b.toInt())
        if (b == endByte) return buffer
      }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readMode(file: File): Int {
      return Files.getAttribute(file.toPath(), "unix:mode") as? Int
        ?: throw RuntimeException("Unable to read file mode")
    }
  }
}
