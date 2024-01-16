package top.saymzx.easycontrol.app.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class BufferNew {
  private final LinkedBlockingDeque<ByteBuffer> dataQueue = new LinkedBlockingDeque<>();

  public void write(ByteBuffer data) {
    dataQueue.offerLast(data);
  }

  public ByteBuffer read(int len) throws InterruptedException {
    if (len < 0) return null;
    ByteBuffer data = ByteBuffer.allocate(len);
    int readBytes = 0;
    synchronized (this) {
      while (readBytes < len) {
        ByteBuffer tmpData = dataQueue.takeFirst();
        int needReadSize = len - readBytes;
        if (tmpData.remaining() > needReadSize) {
          readBytes += needReadSize;
          int oldLimit = tmpData.limit();
          tmpData.limit(tmpData.position() + needReadSize);
          data.put(tmpData);
          tmpData.limit(oldLimit);
          dataQueue.offerFirst(tmpData);
        } else {
          readBytes += tmpData.remaining();
          data.put(tmpData);
        }
      }
    }
    data.flip();
    return data;
  }

  public ByteBuffer readNext() throws InterruptedException {
    synchronized (this) {
      return dataQueue.takeFirst();
    }
  }

  public boolean isEmpty() {
    return dataQueue.isEmpty();
  }

  public int getSize() {
    int size = 0;
    for (ByteBuffer byteBuffer : dataQueue) size += byteBuffer.remaining();
    return size;
  }

}
