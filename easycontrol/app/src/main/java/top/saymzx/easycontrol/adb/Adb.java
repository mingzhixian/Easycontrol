package top.saymzx.easycontrol.adb;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

// 此部分代码摘抄借鉴了tananaev大佬的开源代码(https://github.com/tananaev/adblib)以及开源库dadb(https://github.com/mobile-dev-inc/dadb)
public class Adb {
  private final AdbChannel channel;
  private final Random random = new Random();
  private final ConcurrentHashMap<Integer, AdbStream> connectionStreams = new ConcurrentHashMap<>();
  private final BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>();

  private final Thread handleIn = new Thread(this::handleIn);
  private final Thread handleOut = new Thread(this::handleOut);

  public Adb(String host, int port, AdbKeyPair keyPair) throws Exception {
    channel = new TcpChannel(host, port);
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
    channel.write(AdbProtocol.generateConnect());
    AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
    if (message.command == AdbProtocol.CMD_AUTH) {
      channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message)));
      message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      if (message.command == AdbProtocol.CMD_AUTH) {
        channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes));
        message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      }
    }
    if (message.command != AdbProtocol.CMD_CNXN) throw new Exception("ADB连接失败: " + message.command + "-" + new String(message.payload));
    // 启动后台进程
    handleIn.start();
    handleOut.start();
    startListener.interrupt();
  }

  private AdbStream open(String destination, boolean isNeedSource, boolean canMultipleSend) throws IOException, InterruptedException {
    int localId = random.nextInt();
    sendQueue.offer(AdbProtocol.generateOpen(localId, destination));
    AdbStream stream = new AdbStream(sendQueue, localId, isNeedSource, canMultipleSend);
    connectionStreams.put(localId, stream);
    synchronized (stream) {
      stream.wait();
    }
    if (stream.isError()) throw new IOException("打开通道失败: " + destination);
    return stream;
  }

  public void pushFile(InputStream file, String remotePath) throws InterruptedException, IOException {
    // 打开链接
    AdbStream stream = open("sync:", false, false);
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
    // 传输完成
    stream.write(AdbProtocol.generatePushPacket("DONE", (int) System.currentTimeMillis()));
    stream.write(AdbProtocol.generatePushPacket("QUIT", 0));
    synchronized (stream) {
      stream.wait();
    }
    file.close();
  }

  public String runAdbCmd(String cmd, boolean isNeedOutput) throws IOException, InterruptedException {
    if (!isNeedOutput) {
      if (connectionStreams.get(0) == null) connectionStreams.put(0, open("shell:", false, true));
      connectionStreams.get(0).write(ByteBuffer.wrap((cmd + "\n").getBytes()));
      return "";
    } else {
      AdbStream stream = open("shell:" + cmd, true, true);
      synchronized (stream) {
        stream.wait();
      }
      return stream.readString();
    }
  }

  public AdbStream tcpForward(int port, boolean isNeedSource) throws IOException, InterruptedException {
    return open("tcp:" + port, isNeedSource, true);
  }

  public AdbStream localSocketForward(String socketName, boolean isNeedSource) throws IOException, InterruptedException {
    return open("localabstract:" + socketName, isNeedSource, true);
  }

  private void handleIn() {
    try {
      while (!Thread.interrupted()) {
        AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
        AdbStream stream = connectionStreams.get(message.arg1);
        if (stream == null) return;
        switch (message.command) {
          case AdbProtocol.CMD_OKAY:
            stream.setCanWrite(message.arg0);
            break;
          case AdbProtocol.CMD_WRTE:
            sendQueue.offer(AdbProtocol.generateOkay(message.arg1, message.arg0));
            stream.pushSource(message.payload);
            break;
          case AdbProtocol.CMD_CLSE:
            stream.close();
            break;
        }
      }
    } catch (IOException | InterruptedException ignored) {
      close();
    }
  }

  private void handleOut() {
    try {
      ByteBuffer buffer = ByteBuffer.allocate(8192);
      byte[] bytes;
      while (!Thread.interrupted()) {
        buffer.clear();
        // 阻塞等待数据
        buffer.put(sendQueue.take());
        // 将所有数据清空，以减少写入次数
        while ((bytes = sendQueue.peek()) != null) {
          if (bytes.length > buffer.remaining()) break;
          buffer.put(sendQueue.take());
        }
        buffer.flip();
        bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        channel.write(bytes);
        channel.flush();
      }
    } catch (IOException | InterruptedException ignored) {
      close();
    }
  }

  public void sendMoreOk(AdbStream stream) {
    sendQueue.offer(AdbProtocol.generateOkay(stream.localId, stream.remoteId));
  }

  private boolean isClosed = false;

  public void close() {
    if (isClosed) return;
    isClosed = true;
    handleIn.interrupt();
    handleOut.interrupt();
    for (AdbStream value : connectionStreams.values()) value.close();
    try {
      channel.close();
    } catch (Exception ignored) {
    }
  }
}
