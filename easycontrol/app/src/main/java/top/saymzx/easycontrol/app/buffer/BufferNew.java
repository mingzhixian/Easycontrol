package top.saymzx.easycontrol.app.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class BufferNew {
  private boolean isClosed = false;
  private final LinkedBlockingDeque<ByteBuffer> dataQueue = new LinkedBlockingDeque<>();

  public void write(ByteBuffer data) {
    dataQueue.offerLast(data);
  }

  public synchronized ByteBuffer read(int len) throws InterruptedException, IOException {
    if (len < 0 || isClosed) throw new IOException("BufferNew error");
    ByteBuffer data = ByteBuffer.allocate(len);
    int bytesToRead = len;
    while (bytesToRead > 0) {
      ByteBuffer tmpData = dataQueue.takeFirst();
      if (isClosed) throw new IOException("BufferNew error");
      int remaining = tmpData.remaining();
      if (remaining <= bytesToRead) {
        data.put(tmpData);
        bytesToRead -= remaining;
      } else {
        int oldLimit = tmpData.limit();
        tmpData.limit(tmpData.position() + bytesToRead);
        data.put(tmpData);
        tmpData.limit(oldLimit);
        dataQueue.offerFirst(tmpData);
        bytesToRead = 0;
      }
    }
    data.flip();
    return data;
  }

  public synchronized ByteBuffer readNext() throws InterruptedException, IOException {
    if (isClosed) throw new IOException("BufferNew error");
    ByteBuffer byteBuffer = dataQueue.takeFirst();
    if (isClosed) throw new IOException("BufferNew error");
    return byteBuffer;
  }

  public ByteBuffer readByteArrayBeforeClose() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(Math.max(getSize(), 1));
    for (ByteBuffer tmpBuffer : dataQueue) byteBuffer.put(tmpBuffer);
    return byteBuffer;
  }

  public boolean isEmpty() {
    return dataQueue.isEmpty();
  }

  public int getSize() {
    int size = 0;
    for (ByteBuffer byteBuffer : dataQueue) size += byteBuffer.remaining();
    return size;
  }

  public void close() {
    if (isClosed) return;
    isClosed = true;
    dataQueue.offer(ByteBuffer.allocate(1));
  }

}
