package top.saymzx.adb;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import top.saymzx.buffer.BufferNew;
import top.saymzx.buffer.Stream;

// 此部分代码摘抄借鉴了tananaev大佬的开源代码(https://github.com/tananaev/adblib)以及开源库dadb(https://github.com/mobile-dev-inc/dadb)
public class Adb {
  private final AdbChannel channel;
  private int localIdPool = 1;
  private final ConcurrentHashMap<Integer, Stream> connectionStreams = new ConcurrentHashMap<>();
  private final BufferNew sendBuffer = new BufferNew();

  private final Thread handleInThread = new Thread(this::handleIn);
  private final Thread handleOutThread = new Thread(this::handleOut);

  public Adb(String host, int port, AdbKeyPair keyPair) throws Exception {
    channel = new TcpChannel(host, port);
    connect(keyPair);
  }

  public Adb(Stream stream, AdbKeyPair keyPair) throws Exception {
    channel = new StreamChannel(stream);
    connect(keyPair);
  }

  private void connect(AdbKeyPair keyPair) throws Exception {
    // 授权超时
    Thread startListener = new Thread(() -> {
      try {
        Thread.sleep(10 * 1000);
        close();
      } catch (InterruptedException ignored) {
      }
    });
    startListener.start();
    // 连接ADB并认证
    channel.write(AdbProtocol.generateConnect().array());
    AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
    if (message.command == AdbProtocol.CMD_AUTH) {
      channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message)).array());
      message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      if (message.command == AdbProtocol.CMD_AUTH) {
        channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes).array());
        message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      }
    }
    if (message.command != AdbProtocol.CMD_CNXN) throw new Exception("ADB连接失败: " + message.command + "-" + new String(message.payload));
    // 启动后台进程
    handleInThread.start();
    handleOutThread.start();
    startListener.interrupt();
  }

  private Stream open(String destination, boolean canMultipleSend) throws InterruptedException {
    int localId = localIdPool++ * (canMultipleSend ? 1 : -1);
    sendBuffer.write(AdbProtocol.generateOpen(localId, destination));
    Stream stream;
    do {
      synchronized (this) {
        wait();
      }
      stream = connectionStreams.get(localId);
    } while (stream == null);
    return stream;
  }

  public void pushFile(InputStream file, String remotePath) throws Exception {
    // 打开链接
    Stream stream = open("sync:", false);
    // 发送信令，建立push通道
    String sendString = remotePath + ",33206";
    byte[] bytes = sendString.getBytes();
    ByteBuffer send = ByteBuffer.allocate(bytes.length + 8);
    send.put(AdbProtocol.generatePushPacket("SEND", sendString.length()));
    send.put(bytes);
    send.flip();
    stream.write(send);
    // 发送文件
    byte[] byteArray = new byte[4096 - 8];
    int len = file.read(byteArray, 0, byteArray.length);
    do {
      ByteBuffer data = ByteBuffer.allocate(len + 8);
      data.put(AdbProtocol.generatePushPacket("DATA", len));
      data.put(byteArray, 0, len);
      data.flip();
      stream.write(data);
      len = file.read(byteArray, 0, byteArray.length);
    } while (len > 0);
    file.close();
    // 传输完成
    stream.write(AdbProtocol.generatePushPacket("DONE", (int) System.currentTimeMillis()));
    stream.write(AdbProtocol.generatePushPacket("QUIT", 0));
    do {
      synchronized (this) {
        wait();
      }
    } while (!stream.isClosed());
  }

  public String runAdbCmd(String cmd, boolean waitOutput) throws Exception {
    if (waitOutput) {
      Stream stream = open("shell:" + cmd, true);
      do {
        synchronized (this) {
          wait();
        }
      } while (!stream.isClosed());
      return new String(stream.readAllBytes().array());
    } else {
      Stream stream = open("shell:", true);
      stream.write(ByteBuffer.wrap((cmd + "\n").getBytes()));
      return "";
    }
  }

  public Stream tcpForward(int port) throws IOException, InterruptedException {
    Stream stream = open("tcp:" + port, true);
    if (stream.isClosed()) throw new IOException("error forward");
    return stream;
  }

  public Stream localSocketForward(String socketName) throws IOException, InterruptedException {
    Stream stream = open("localabstract:" + socketName, true);
    if (stream.isClosed()) throw new IOException("error forward");
    return stream;
  }

  private void handleIn() {
    try {
      while (!Thread.interrupted()) {
        AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
        Stream stream = connectionStreams.get(message.arg1);
        boolean isNeedNotify = stream == null;
        // 新连接
        if (isNeedNotify) {
          stream = createNewStream(message.arg1, message.arg0, message.arg1 > 0);
          connectionStreams.put(message.arg1, stream);
        }
        switch (message.command) {
          case AdbProtocol.CMD_OKAY:
            stream.setCanWrite(true);
            break;
          case AdbProtocol.CMD_WRTE:
            sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
            stream.pushSource(message.payload);
            // sendMoreOk
            sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
            break;
          case AdbProtocol.CMD_CLSE:
            stream.close();
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
        channel.write(sendBuffer.readNext().array());
        if (!sendBuffer.isEmpty()) channel.write(sendBuffer.read(sendBuffer.getSize()).array());
        channel.flush();
      }
    } catch (IOException | InterruptedException ignored) {
      close();
    }
  }

  private Stream createNewStream(int localId, int remoteId, boolean canMultipleSend) {
    return new Stream(false, canMultipleSend, new Stream.UnderlySocketFunction() {
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
