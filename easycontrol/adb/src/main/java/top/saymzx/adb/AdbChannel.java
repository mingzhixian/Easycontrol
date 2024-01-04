package top.saymzx.adb;

import java.io.IOException;

public interface AdbChannel {
  void write(byte[] data) throws IOException, InterruptedException;

  void flush() throws IOException;

  byte[] read(int size) throws IOException, InterruptedException;

  void close();

}
