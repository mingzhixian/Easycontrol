package top.saymzx.easycontrol.app.adb;

import android.hardware.usb.UsbDevice;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import top.saymzx.easycontrol.app.buffer.BufferStream;
import top.saymzx.easycontrol.app.entity.MyInterface;

// 此部分代码摘抄借鉴了tananaev大佬的开源代码(https://github.com/tananaev/adblib)以及开源库dadb(https://github.com/mobile-dev-inc/dadb)
// 因为官方adb协议文档写的十分糟糕，因此此部分代码的实现参考了cstyan大佬所整理的文档，再次进行感谢：https://github.com/cstyan/adbDocumentation
public class Adb {
  private boolean isClosed = false;
  private final AdbChannel channel;
  private int localIdPool = 1;
  private int MAX_DATA = AdbProtocol.CONNECT_MAXDATA;
  private final ConcurrentHashMap<Integer, BufferStream> connectionStreams = new ConcurrentHashMap<>(10);
  private final ConcurrentHashMap<Integer, BufferStream> openStreams = new ConcurrentHashMap<>(5);

  private final Thread handleInThread = new Thread(this::handleIn);

  public Adb(String address,int port, AdbKeyPair keyPair) throws Exception {
    channel = new TcpChannel(address, port);
    connect(keyPair);
  }

  public Adb(UsbDevice usbDevice, AdbKeyPair keyPair) throws Exception {
    if (usbDevice == null) throw new IOException("no usb connect");
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
    if (message.command != AdbProtocol.CMD_CNXN) {
      channel.close();
      throw new Exception("ADB连接失败");
    }
    MAX_DATA = message.arg1;
    // 启动后台进程
    handleInThread.setPriority(Thread.MAX_PRIORITY);
    handleInThread.start();
  }

  private BufferStream open(String destination, boolean canMultipleSend) throws InterruptedException {
    int localId = localIdPool++ * (canMultipleSend ? 1 : -1);
    writeToChannel(AdbProtocol.generateOpen(localId, destination));
    BufferStream bufferStream;
    do {
      synchronized (this) {
        wait();
      }
      bufferStream = openStreams.get(localId);
    } while (!isClosed && bufferStream == null);
    openStreams.remove(localId);
    return bufferStream;
  }

  public String restartOnTcpip(int port) throws InterruptedException {
    BufferStream bufferStream = open("tcpip:" + port, false);
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
    return new String(bufferStream.readByteArrayBeforeClose().array());
  }

  public void pushFile(InputStream file, String remotePath, MyInterface.MyFunctionInt handleProcess) throws Exception {
    // 打开链接
    BufferStream bufferStream = open("sync:", false);
    // 发送信令，建立push通道
    String sendString = remotePath + ",33206";
    byte[] bytes = sendString.getBytes();
    bufferStream.write(AdbProtocol.generateSyncHeader("SEND", sendString.length()));
    bufferStream.write(ByteBuffer.wrap(bytes));
    // 发送文件
    byte[] byteArray = new byte[10240 - 8];
    int hasSendLen = 0;
    int allNeedSendLen = file.available();
    int lastProcess = 0;
    int len = file.read(byteArray, 0, byteArray.length);
    do {
      bufferStream.write(AdbProtocol.generateSyncHeader("DATA", len));
      bufferStream.write(ByteBuffer.wrap(byteArray, 0, len));
      hasSendLen += len;
      int newProcess = (int) (((float) hasSendLen / allNeedSendLen) * 100);
      if (newProcess != lastProcess) {
        lastProcess = newProcess;
        if (handleProcess != null) handleProcess.run(lastProcess);
      }
      len = file.read(byteArray, 0, byteArray.length);
    } while (len > 0);
    file.close();
    // 传输完成，为了方便，文件日期定为2024.1.1 0:0
    bufferStream.write(AdbProtocol.generateSyncHeader("DONE", 1704038400));
    bufferStream.write(AdbProtocol.generateSyncHeader("QUIT", 0));
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
    return new String(bufferStream.readByteArrayBeforeClose().array());
  }

  public BufferStream getShell() throws InterruptedException {
    return open("shell:", true);
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
        if (isNeedNotify) bufferStream = createNewStream(message.arg1, message.arg0, message.arg1 > 0);
        switch (message.command) {
          case AdbProtocol.CMD_OKAY:
            bufferStream.setCanWrite(true);
            break;
          case AdbProtocol.CMD_WRTE:
            bufferStream.pushSource(message.payload);
            writeToChannel(AdbProtocol.generateOkay(message.arg1, message.arg0));
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

  private void writeToChannel(ByteBuffer byteBuffer) {
    synchronized (channel) {
      try {
        channel.write(byteBuffer);
      } catch (Exception ignored) {
        close();
      }
    }
  }

  private BufferStream createNewStream(int localId, int remoteId, boolean canMultipleSend) throws Exception {
    return new BufferStream(false, canMultipleSend, new BufferStream.UnderlySocketFunction() {
      @Override
      public void connect(BufferStream bufferStream) {
        connectionStreams.put(localId, bufferStream);
        openStreams.put(localId, bufferStream);
      }

      @Override
      public void write(BufferStream bufferStream, ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
          byte[] byteArray = new byte[Math.min(MAX_DATA - 128, buffer.remaining())];
          buffer.get(byteArray);
          writeToChannel(AdbProtocol.generateWrite(localId, remoteId, byteArray));
        }
      }

      @Override
      public void flush(BufferStream bufferStream) {
        writeToChannel(AdbProtocol.generateOkay(localId, remoteId));
      }

      @Override
      public void close(BufferStream bufferStream) {
        connectionStreams.remove(localId);
        writeToChannel(AdbProtocol.generateClose(localId, remoteId));
      }
    });
  }

  public boolean isClosed() {
    return isClosed;
  }

  public void close() {
    if (isClosed) return;
    isClosed = true;
    handleInThread.interrupt();
    for (Object bufferStream : connectionStreams.values().toArray()) ((BufferStream) bufferStream).close();
    channel.close();
    synchronized (this) {
      notifyAll();
    }
  }

}
