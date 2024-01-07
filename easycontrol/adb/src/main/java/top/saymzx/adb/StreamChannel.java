package top.saymzx.adb;

import java.io.IOException;
import java.nio.ByteBuffer;

import top.saymzx.buffer.Stream;

public class StreamChannel implements AdbChannel {

  private final Stream stream;

  public StreamChannel(Stream stream) {
    this.stream = stream;
  }

  @Override
  public void write(byte[] data) throws IOException, InterruptedException {
    stream.write(ByteBuffer.wrap(data));
  }

  @Override
  public void flush() {
  }

  @Override
  public byte[] read(int size) throws IOException, InterruptedException {
    return stream.readByteArray(size).array();
  }

  @Override
  public void close() {
    stream.close();
  }
}
