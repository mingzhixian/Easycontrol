package top.saymzx.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class BufferNew {
  private final LinkedBlockingDeque<ByteBuffer> dataQueue = new LinkedBlockingDeque<>();

  public void write(ByteBuffer data) {
    dataQueue.offerLast(data);
  }

  public ByteBuffer read(int size) throws InterruptedException {
    ByteBuffer data = ByteBuffer.allocate(size);
    int readBytes = 0;
    synchronized (dataQueue) {
      while (readBytes < size) {
        ByteBuffer tmpData = dataQueue.takeFirst();
        int needReadSize = size - readBytes;
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

  // 队列为空时返回null
  public ByteBuffer readNext() {
    return dataQueue.pollFirst();
  }

  public boolean isEmpty() {
    return dataQueue.isEmpty();
  }

  // 此操作速度较慢，慎用
  public int getSize() {
    int size = 0;
    for (ByteBuffer byteBuffer : dataQueue) size += byteBuffer.remaining();
    return size;
  }

}
