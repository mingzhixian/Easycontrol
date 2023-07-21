/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.scrcpy.adb

internal object Constants {

  const val AUTH_TYPE_TOKEN = 1
  const val AUTH_TYPE_SIGNATURE = 2
  const val AUTH_TYPE_RSA_PUBLIC = 3

  const val CMD_AUTH = 0x48545541
  const val CMD_CNXN = 0x4e584e43
  const val CMD_OPEN = 0x4e45504f
  const val CMD_OKAY = 0x59414b4f
  const val CMD_CLSE = 0x45534c43
  const val CMD_WRTE = 0x45545257

  const val CONNECT_VERSION = 0x01000000
  const val CONNECT_MAXDATA = 1024 * 1024

  val CONNECT_PAYLOAD = "host::\u0000".toByteArray()
}
