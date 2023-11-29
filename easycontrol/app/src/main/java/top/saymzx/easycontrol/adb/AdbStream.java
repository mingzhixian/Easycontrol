package top.saymzx.easycontrol.adb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

import top.saymzx.buffer.BufferNew;

public class AdbStream {
  private final BlockingQueue<byte[]> sendQueue;
  final int localId;
  // -2为连接错误，-1为已关闭，0为连接中，大于0为已连接
  int remoteId = 0;

  private final boolean isNeedSource;
  private final BufferNew source = new BufferNew();
  private final boolean canMultipleSend;
  private final BufferNew sink = new BufferNew();
  private boolean canWrite = false;

  public AdbStream(BlockingQueue<byte[]> sendQueue, int localId, boolean isNeedSource, boolean canMultipleSend) {
    this.sendQueue = sendQueue;
    this.localId = localId;
    this.isNeedSource = isNeedSource;
    this.canMultipleSend = canMultipleSend;
  }

  public void pushSource(byte[] bytes) {
    if (isNeedSource) source.write(ByteBuffer.wrap(bytes));
  }

  private ByteBuffer pollSink() throws InterruptedException {
    if (sink.isEmpty()) return null;
    // 此处打包发送，以减少ADB同步阻塞带来的延迟
    if (canMultipleSend) return sink.read(sink.getSize());
    else return sink.readNext();
  }

  public int readInt() throws InterruptedException, IOException {
    return readByteArray(4).getInt();
  }

  public long readLong() throws InterruptedException, IOException {
    return readByteArray(8).getLong();
  }

  public byte readByte() throws InterruptedException, IOException {
    return readByteArray(1).get();
  }

  public ByteBuffer readAllBytes() throws InterruptedException, IOException {
    return readByteArray(source.getSize());
  }

  public String readString() throws InterruptedException, IOException {
    return new String(readAllBytes().array(), StandardCharsets.UTF_8);
  }

  public ByteBuffer readByteArray(int size) throws InterruptedException, IOException {
    if (remoteId <= 0 && size > getSize()) throw new IOException("未连接");
    return source.read(size);
  }

  public void write(ByteBuffer byteBuffer) throws IOException, InterruptedException {
    if (remoteId <= 0) throw new IOException("未连接");
    sink.write(byteBuffer);
    // 如果可以则插入发送队列
    checkSend();
  }

  public void setCanWrite(int remoteId) throws InterruptedException {
    if (this.remoteId == 0) {
      this.remoteId = remoteId;
      synchronized (this) {
        notifyAll();
      }
    }
    canWrite = true;
    checkSend();
  }

  private synchronized void checkSend() throws InterruptedException {
    if (canWrite) {
      ByteBuffer data = pollSink();
      if (data != null) {
        canWrite = false;
        sendQueue.offer(AdbProtocol.generateWrite(localId, remoteId, data.array()));
      }
    }
  }

  public boolean isEmpty() {
    return source.isEmpty();
  }

  public int getSize() {
    return source.getSize();
  }

  public boolean isError() {
    return remoteId == -2;
  }

  public void close() {
    if (remoteId < 0) return;
    if (remoteId == 0) remoteId = -2;
    else {
      sendQueue.offer(AdbProtocol.generateClose(localId, remoteId));
      remoteId = -1;
    }
    synchronized (this) {
      notifyAll();
    }
  }
}
