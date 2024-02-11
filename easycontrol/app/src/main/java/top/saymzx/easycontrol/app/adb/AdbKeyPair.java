package top.saymzx.easycontrol.app.adb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

public class AdbKeyPair {

  private final PrivateKey privateKey;
  public final byte[] publicKeyBytes;

  public AdbKeyPair(PrivateKey privateKey, byte[] publicKeyBytes) {
    this.privateKey = privateKey;
    this.publicKeyBytes = publicKeyBytes;
  }

  public byte[] signPayload(ByteBuffer payload) throws Exception {
    if (payload == null) return new byte[]{0};
    Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, privateKey);
    cipher.update(SIGNATURE_PADDING);
    return cipher.doFinal(payload.array());
  }

  public static void setAdbBase64(AdbBase64 adbBase64) {
    AdbKeyPair.adbBase64 = adbBase64;
  }

  public static AdbKeyPair read(File publicKey, File privateKey) throws Exception {
    if (adbBase64 == null) throw new IOException("no adbBase64");
    byte[] publicKeyBytes = new byte[(int) publicKey.length() + 1];
    byte[] privateKeyBytes = new byte[(int) privateKey.length()];
    PrivateKey tmpPrivateKey;

    try (FileInputStream stream = new FileInputStream(publicKey)) {
      stream.read(publicKeyBytes);
      publicKeyBytes[publicKeyBytes.length - 1] = 0;
    }
    try (FileInputStream stream = new FileInputStream(privateKey)) {
      stream.read(privateKeyBytes);
      String data = new String(privateKeyBytes).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("\n", "");
      privateKeyBytes = adbBase64.decode(data.getBytes());
    }

    tmpPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

    return new AdbKeyPair(tmpPrivateKey, publicKeyBytes);
  }

  public static void generate(File publicKey, File privateKey) throws Exception {
    if (adbBase64 == null) throw new IOException("no adbBase64");
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(KEY_LENGTH_BITS);
    KeyPair keyPair = keyPairGenerator.genKeyPair();

    try (FileWriter publicKeyWriter = new FileWriter(publicKey)) {
      publicKeyWriter.write(adbBase64.encodeToString(convertRsaPublicKeyToAdbFormat((RSAPublicKey) keyPair.getPublic())).replace("\n", ""));
      publicKeyWriter.write(" one@Aphone");
      publicKeyWriter.flush();
    }
    try (FileWriter privateKeyWriter = new FileWriter(privateKey)) {
      privateKeyWriter.write("-----BEGIN PRIVATE KEY-----\n");
      privateKeyWriter.write(adbBase64.encodeToString(keyPair.getPrivate().getEncoded()).replace("\n", ""));
      privateKeyWriter.write("\n-----END PRIVATE KEY-----");
      privateKeyWriter.flush();
    }
  }

  private static byte[] convertRsaPublicKeyToAdbFormat(RSAPublicKey pubkey) {
    BigInteger r32, r, rr, rem, n, n0inv;

    r32 = BigInteger.ZERO.setBit(32);
    n = pubkey.getModulus();
    r = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32);
    rr = r.modPow(BigInteger.valueOf(2), n);
    rem = n.remainder(r32);
    n0inv = rem.modInverse(r32);

    int[] myN = new int[KEY_LENGTH_WORDS];
    int[] myRr = new int[KEY_LENGTH_WORDS];
    BigInteger[] res;
    for (int i = 0; i < KEY_LENGTH_WORDS; i++) {
      res = rr.divideAndRemainder(r32);
      rr = res[0];
      rem = res[1];
      myRr[i] = rem.intValue();

      res = n.divideAndRemainder(r32);
      n = res[0];
      rem = res[1];
      myN[i] = rem.intValue();
    }

    ByteBuffer bbuf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
    bbuf.putInt(KEY_LENGTH_WORDS);
    bbuf.putInt(n0inv.negate().intValue());
    for (int i : myN) bbuf.putInt(i);
    for (int i : myRr) bbuf.putInt(i);

    bbuf.putInt(pubkey.getPublicExponent().intValue());
    return bbuf.array();
  }


  private static final int KEY_LENGTH_BITS = 2048;
  private static final int KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8;
  private static final int KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4;
  private static AdbBase64 adbBase64;

  public static final byte[] SIGNATURE_PADDING = new byte[]{
    (byte) 0x00, (byte) 0x01, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
    (byte) 0x30, (byte) 0x21, (byte) 0x30, (byte) 0x09, (byte) 0x06, (byte) 0x05, (byte) 0x2b, (byte) 0x0e, (byte) 0x03, (byte) 0x02, (byte) 0x1a, (byte) 0x05, (byte) 0x00,
    (byte) 0x04, (byte) 0x14
  };

}
