/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.easycontrol.adb

import android.util.Base64
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

internal object PKCS8 {

  private const val PREFIX = "-----BEGIN PRIVATE KEY-----"
  private const val SUFFIX = "-----END PRIVATE KEY-----"

  fun parse(bytes: ByteArray): PrivateKey {
    val string = String(bytes).replace(PREFIX, "").replace(SUFFIX, "").replace("\n", "")
    val encoded = Base64.decode(string, 0)
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = PKCS8EncodedKeySpec(encoded)
    return keyFactory.generatePrivate(keySpec)
  }
}