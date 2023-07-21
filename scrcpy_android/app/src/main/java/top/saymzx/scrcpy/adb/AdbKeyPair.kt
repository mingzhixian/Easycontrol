/*
 * 本项目为适用于安卓的ADB库，本项目大量借鉴学习了开源ADB库：Dadb，在此对该项目表示感谢
 */

package top.saymzx.scrcpy.adb

import android.util.Base64
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

class AdbKeyPair(
  private val privateKey: PrivateKey,
  internal val publicKeyBytes: ByteArray
) {

  internal fun signPayload(message: AdbMessage): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, privateKey)
    cipher.update(SIGNATURE_PADDING)
    return cipher.doFinal(message.payload, 0, message.payloadLength)
  }

  companion object {

    private const val KEY_LENGTH_BITS = 2048
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8
    private const val KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4

    private val SIGNATURE_PADDING = ubyteArrayOf(
      0x00u, 0x01u, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
      0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0x00u,
      0x30u, 0x21u, 0x30u, 0x09u, 0x06u, 0x05u, 0x2bu, 0x0eu, 0x03u, 0x02u, 0x1au, 0x05u, 0x00u,
      0x04u, 0x14u
    ).toByteArray()

    @JvmStatic
    @JvmOverloads
    fun read(privateKeyFile: File, publicKeyFile: File? = null): AdbKeyPair {
      val privateKey = PKCS8.parse(privateKeyFile.readBytes())
      val publicKeyBytes = if (publicKeyFile?.exists() == true) {
        readAdbPublicKey(publicKeyFile)
      } else {
        ByteArray(0)
      }

      return AdbKeyPair(privateKey, publicKeyBytes)
    }

    @JvmStatic
    fun generate(privateKeyFile: File, publicKeyFile: File) {
      val keyPair = KeyPairGenerator.getInstance("RSA").let {
        it.initialize(KEY_LENGTH_BITS)
        it.genKeyPair()
      }

      privateKeyFile.absoluteFile.parentFile?.mkdirs()
      publicKeyFile.absoluteFile.parentFile?.mkdirs()

      privateKeyFile.writer().use { out ->
        out.write("-----BEGIN PRIVATE KEY-----\n")
        out.write(Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
        out.write("\n-----END PRIVATE KEY-----")
        out.flush()
      }

      publicKeyFile.writer().use { out ->
        val bytes = convertRsaPublicKeyToAdbFormat(keyPair.public as RSAPublicKey)
        out.write(Base64.encodeToString(bytes, Base64.NO_WRAP))
        out.write(" scrcpy@saymzx.top")
        out.flush()
      }
    }

    private fun readAdbPublicKey(file: File): ByteArray {
      val bytes = file.readBytes()
      val publicKeyBytes = bytes.copyOf(bytes.size + 1)
      publicKeyBytes[bytes.size] = 0
      return publicKeyBytes
    }

    // https://github.com/cgutman/AdbLib/blob/d6937951eb98557c76ee2081e383d50886ce109a/src/com/cgutman/adblib/AdbCrypto.java#L83-L137
    @Suppress("JoinDeclarationAndAssignment")
    private fun convertRsaPublicKeyToAdbFormat(pubkey: RSAPublicKey): ByteArray {
      /*
       * ADB literally just saves the RSAPublicKey struct to a file.
       *
       * typedef struct RSAPublicKey {
       * int len; // Length of n[] in number of uint32_t
       * uint32_t n0inv;  // -1 / n[0] mod 2^32
       * uint32_t n[RSANUMWORDS]; // modulus as little endian array
       * uint32_t rr[RSANUMWORDS]; // R^2 as little endian array
       * int exponent; // 3 or 65537
       * } RSAPublicKey;
       */

      /* ------ This part is a Java-ified version of RSA_to_RSAPublicKey from adb_host_auth.c ------ */
      val r32: BigInteger
      val r: BigInteger
      var rr: BigInteger
      var rem: BigInteger
      var n: BigInteger
      val n0inv: BigInteger
      r32 = BigInteger.ZERO.setBit(32)
      n = pubkey.modulus
      r = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32)
      rr = r.modPow(BigInteger.valueOf(2), n)
      rem = n.remainder(r32)
      n0inv = rem.modInverse(r32)
      val myN = IntArray(KEY_LENGTH_WORDS)
      val myRr = IntArray(KEY_LENGTH_WORDS)
      var res: Array<BigInteger>
      for (i in 0 until KEY_LENGTH_WORDS) {
        res = rr.divideAndRemainder(r32)
        rr = res[0]
        rem = res[1]
        myRr[i] = rem.toInt()
        res = n.divideAndRemainder(r32)
        n = res[0]
        rem = res[1]
        myN[i] = rem.toInt()
      }

      /* ------------------------------------------------------------------------------------------- */
      val bbuf: ByteBuffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
      bbuf.putInt(KEY_LENGTH_WORDS)
      bbuf.putInt(n0inv.negate().toInt())
      for (i in myN) bbuf.putInt(i)
      for (i in myRr) bbuf.putInt(i)
      bbuf.putInt(pubkey.publicExponent.toInt())
      return bbuf.array()
    }
  }
}
