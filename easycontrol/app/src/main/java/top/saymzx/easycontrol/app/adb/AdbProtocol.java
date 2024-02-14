package top.saymzx.easycontrol.app.adb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class AdbProtocol {
  public static final int ADB_HEADER_LENGTH = 24;

  public static final int AUTH_TYPE_TOKEN = 1;
  public static final int AUTH_TYPE_SIGNATURE = 2;
  public static final int AUTH_TYPE_RSA_PUBLIC = 3;

  public static final int CMD_AUTH = 0x48545541;
  public static final int CMD_CNXN = 0x4e584e43;
  public static final int CMD_OPEN = 0x4e45504f;
  public static final int CMD_OKAY = 0x59414b4f;
  public static final int CMD_CLSE = 0x45534c43;
  public static final int CMD_WRTE = 0x45545257;

  public static final int CONNECT_VERSION = 0x01000000;
  // 最大数据大小一般为1024*1024，此处设置为15*1024，是因为有些设备USB仅支持最大16*1024，所以如果ADB使用了过大的数据，会导致USB无法传输，丢失数据，所以限制ADB协议最大为15k
  // 旧版本的adb服务端硬编码maxdata=4096，若设备实在太老，可尝试将此处修改为4096
  public static final int CONNECT_MAXDATA = 15 * 1024;

  public static final byte[] CONNECT_PAYLOAD = "host::\0".getBytes();

  public static ByteBuffer generateConnect() {
    return generateMessage(CMD_CNXN, CONNECT_VERSION, CONNECT_MAXDATA, CONNECT_PAYLOAD);
  }

  public static ByteBuffer generateAuth(int type, byte[] data) {
    return generateMessage(CMD_AUTH, type, 0, data);
  }

  public static ByteBuffer generateOpen(int localId, String dest) {
    ByteBuffer bbuf = ByteBuffer.allocate(dest.length() + 1);
    bbuf.put(dest.getBytes(StandardCharsets.UTF_8));
    bbuf.put((byte) 0);
    return generateMessage(CMD_OPEN, localId, 0, bbuf.array());
  }

  public static ByteBuffer generateWrite(int localId, int remoteId, byte[] data) {
    return generateMessage(CMD_WRTE, localId, remoteId, data);
  }

  public static ByteBuffer generateClose(int localId, int remoteId) {
    return generateMessage(CMD_CLSE, localId, remoteId, null);
  }

  public static ByteBuffer generateOkay(int localId, int remoteId) {
    return generateMessage(CMD_OKAY, localId, remoteId, null);
  }

  private static ByteBuffer generateMessage(int cmd, int arg0, int arg1, byte[] payload) {

    int size = payload == null ? ADB_HEADER_LENGTH : (ADB_HEADER_LENGTH + payload.length);
    ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

    buffer.putInt(cmd);
    buffer.putInt(arg0);
    buffer.putInt(arg1);

    if (payload == null) {
      buffer.putInt(0);
      buffer.putInt(0);
    } else {
      buffer.putInt(payload.length);
      buffer.putInt(payloadChecksum(payload));
    }

    buffer.putInt(~cmd);
    if (payload != null) buffer.put(payload);
    buffer.flip();

    return buffer;
  }

  public static ByteBuffer generateSyncHeader(String id, int arg) {
    ByteBuffer tmpBuffer = ByteBuffer.allocate(8);
    tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);
    tmpBuffer.clear();
    tmpBuffer.put(id.getBytes(StandardCharsets.UTF_8));
    tmpBuffer.putInt(arg);
    tmpBuffer.flip();
    return tmpBuffer;
  }

  private static int payloadChecksum(byte[] payload) {
    int checksum = 0;
    for (byte b : payload) checksum += (b & 0xFF);
    return checksum;
  }

  final static class AdbMessage {
    public int command;
    public int arg0;
    public int arg1;
    public int payloadLength;
    public ByteBuffer payload = null;

    public static AdbMessage parseAdbMessage(AdbChannel channel) throws IOException, InterruptedException {
      AdbMessage msg = new AdbMessage();
      ByteBuffer buffer = channel.read(ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);

      msg.command = buffer.getInt();
      msg.arg0 = buffer.getInt();
      msg.arg1 = buffer.getInt();
      msg.payloadLength = buffer.getInt();
//      msg.checksum = buffer.getInt();
//      msg.magic = buffer.getInt();
      if (msg.payloadLength > 0) msg.payload = channel.read(msg.payloadLength);

      return msg;
    }
  }
}
