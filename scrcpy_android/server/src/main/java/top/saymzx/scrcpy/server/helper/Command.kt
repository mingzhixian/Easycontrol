package top.saymzx.scrcpy.server.helper

import java.io.IOException
import java.util.Arrays
import java.util.Scanner

object Command {
  @Throws(IOException::class, InterruptedException::class)
  fun exec(vararg cmd: String?) {
    val process = Runtime.getRuntime().exec(cmd)
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw IOException("Command " + Arrays.toString(cmd) + " returned with value " + exitCode)
    }
  }

  @Throws(IOException::class, InterruptedException::class)
  fun execReadLine(vararg cmd: String?): String? {
    var result: String? = null
    val process = Runtime.getRuntime().exec(cmd)
    val scanner = Scanner(process.inputStream)
    if (scanner.hasNextLine()) {
      result = scanner.nextLine()
    }
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw IOException("Command " + Arrays.toString(cmd) + " returned with value " + exitCode)
    }
    return result
  }

  @JvmStatic
  @Throws(IOException::class, InterruptedException::class)
  fun execReadOutput(vararg cmd: String?): String {
    val process = Runtime.getRuntime().exec(cmd)
    val builder = StringBuilder()
    val scanner = Scanner(process.inputStream)
    while (scanner.hasNextLine()) {
      builder.append(scanner.nextLine()).append('\n')
    }
    val output = builder.toString()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw IOException("Command " + Arrays.toString(cmd) + " returned with value " + exitCode)
    }
    return output
  }
}