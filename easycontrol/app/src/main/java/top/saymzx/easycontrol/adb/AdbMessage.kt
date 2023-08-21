/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

internal class AdbMessage(
  val command: Int,
  val arg0: Int,
  val arg1: Int,
  val payloadLength: Int,
  val payload: ByteArray
)
