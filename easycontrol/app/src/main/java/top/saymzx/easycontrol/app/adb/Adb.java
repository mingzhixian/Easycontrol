package top.saymzx.easycontrol.app.adb;

import android.hardware.usb.UsbDevice;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import top.saymzx.easycontrol.app.buffer.BufferNew;
import top.saymzx.easycontrol.app.buffer.BufferStream;

// 此部分代码摘抄借鉴了tananaev大佬的开源代码(https://github.com/tananaev/adblib)以及开源库dadb(https://github.com/mobile-dev-inc/dadb)
public class Adb {
  private final AdbChannel channel;
  private int localIdPool = 1;
  private final ConcurrentHashMap<Integer, BufferStream> connectionStreams = new ConcurrentHashMap<>();
  private final BufferNew sendBuffer = new BufferNew();

  private final Thread handleInThread = new Thread(this::handleIn);
  private final Thread handleOutThread = new Thread(this::handleOut);

  public Adb(String host, int port, AdbKeyPair keyPair) throws Exception {
    channel = new TcpChannel(host, port);
    connect(keyPair);
  }

  public Adb(UsbDevice usbDevice, AdbKeyPair keyPair) throws Exception {
    channel = new UsbChannel(usbDevice);
    connect(keyPair);
  }

  private void connect(AdbKeyPair keyPair) throws Exception {
    // 连接ADB并认证
    channel.write(AdbProtocol.generateConnect());
    AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
    if (message.command == AdbProtocol.CMD_AUTH) {
      channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message.payload)));
      message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      if (message.command == AdbProtocol.CMD_AUTH) {
        channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes));
        message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      }
    }
    if (message.command != AdbProtocol.CMD_CNXN) throw new Exception("ADB连接失败");
    // 启动后台进程
    handleInThread.start();
    handleOutThread.start();
  }

  private BufferStream open(String destination, boolean canMultipleSend) throws InterruptedException {
    int localId = localIdPool++ * (canMultipleSend ? 1 : -1);
    sendBuffer.write(AdbProtocol.generateOpen(localId, destination));
    BufferStream bufferStream;
    do {
      synchronized (this) {
        wait();
      }
      bufferStream = connectionStreams.get(localId);
    } while (bufferStream == null);
    return bufferStream;
  }

  public String restartOnTcpip(int port) throws InterruptedException, IOException {
    BufferStream bufferStream = open("tcpip:" + port, false);
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
    return new String(bufferStream.readAllBytes().array());
  }

  public void pushFileByShell(InputStream file, String remotePath) throws Exception {
    BufferStream bufferStream = open("shell:", false);
    bufferStream.write(ByteBuffer.wrap(("rm " + remotePath + ".tmp \n").getBytes()));
    // 读取数据并转码
    byte[] fileBytes = new byte[file.available()];
    file.read(fileBytes);
    file.close();
    String fileBase64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP);
    // 分段传输
    for (int start = 0; start < fileBase64.length(); start += 2048) {
      String fileBase64Part = fileBase64.substring(start, Math.min(fileBase64.length(), start + 2048));
      bufferStream.write(ByteBuffer.wrap(("echo -n \"" + fileBase64Part + "\" >> " + remotePath + ".tmp \n").getBytes()));
    }
    // 关闭传输
    bufferStream.write(ByteBuffer.wrap((" base64 -d < " + remotePath + ".tmp > " + remotePath + " \n ").getBytes()));
    Thread.sleep(500);
    bufferStream.close();
  }

  public void pushFile(InputStream file, String remotePath) throws Exception {
    // 打开链接
    BufferStream bufferStream = open("sync:", false);
    // 发送信令，建立push通道
    String sendString = remotePath + ",33206";
    byte[] bytes = sendString.getBytes();
    ByteBuffer send = ByteBuffer.allocate(bytes.length + 8);
    send.put(AdbProtocol.generatePushPacket("SEND", sendString.length()));
    send.put(bytes);
    send.flip();
    bufferStream.write(send);
    // 发送文件
    byte[] byteArray = new byte[4096 - 8];
    int len = file.read(byteArray, 0, byteArray.length);
    do {
      ByteBuffer data = ByteBuffer.allocate(len + 8);
      data.put(AdbProtocol.generatePushPacket("DATA", len));
      data.put(byteArray, 0, len);
      data.flip();
      bufferStream.write(data);
      len = file.read(byteArray, 0, byteArray.length);
    } while (len > 0);
    file.close();
    // 传输完成
    bufferStream.write(AdbProtocol.generatePushPacket("DONE", (int) System.currentTimeMillis()));
    bufferStream.write(AdbProtocol.generatePushPacket("QUIT", 0));
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
  }

  public String runAdbCmd(String cmd) throws Exception {
    BufferStream bufferStream = open("shell:" + cmd, true);
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
    return new String(bufferStream.readAllBytes().array());
  }

  public BufferStream getShell() throws InterruptedException {
    return open("shell:", false);
  }

  public BufferStream tcpForward(int port) throws IOException, InterruptedException {
    BufferStream bufferStream = open("tcp:" + port, true);
    if (bufferStream.isClosed()) throw new IOException("error forward");
    return bufferStream;
  }

  public BufferStream localSocketForward(String socketName) throws IOException, InterruptedException {
    BufferStream bufferStream = open("localabstract:" + socketName, true);
    if (bufferStream.isClosed()) throw new IOException("error forward");
    return bufferStream;
  }

  private void handleIn() {
    try {
      while (!Thread.interrupted()) {
        AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
        BufferStream bufferStream = connectionStreams.get(message.arg1);
        boolean isNeedNotify = bufferStream == null;
        // 新连接
        if (isNeedNotify) {
          bufferStream = createNewStream(message.arg1, message.arg0, message.arg1 > 0);
          connectionStreams.put(message.arg1, bufferStream);
        }
        switch (message.command) {
          case AdbProtocol.CMD_OKAY:
            bufferStream.setCanWrite(true);
            break;
          case AdbProtocol.CMD_WRTE:
            sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
            bufferStream.pushSource(message.payload);
            // sendMoreOk
            sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
            break;
          case AdbProtocol.CMD_CLSE:
            bufferStream.close();
            isNeedNotify = true;
            break;
        }
        if (isNeedNotify) {
          synchronized (this) {
            notifyAll();
          }
        }
      }
    } catch (Exception ignored) {
      close();
    }
  }

  private void handleOut() {
    try {
      while (!Thread.interrupted()) {
        channel.write(sendBuffer.readNext());
        if (!sendBuffer.isEmpty()) channel.write(sendBuffer.read(sendBuffer.getSize()));
        channel.flush();
      }
    } catch (IOException | InterruptedException ignored) {
      close();
    }
  }

  private BufferStream createNewStream(int localId, int remoteId, boolean canMultipleSend) {
    return new BufferStream(false, canMultipleSend, new BufferStream.UnderlySocketFunction() {
      @Override
      public void write(ByteBuffer buffer) {
        sendBuffer.write(AdbProtocol.generateWrite(localId, remoteId, buffer.array()));
      }

      @Override
      public void close() {
        sendBuffer.write(AdbProtocol.generateClose(localId, remoteId));
      }
    });
  }

  public void close() {
    handleInThread.interrupt();
    handleOutThread.interrupt();
    channel.close();
  }

}
