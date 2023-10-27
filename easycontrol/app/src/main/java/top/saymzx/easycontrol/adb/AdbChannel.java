package top.saymzx.easycontrol.adb;

import java.io.IOException;

public interface AdbChannel {
  void write(byte[] data) throws IOException;

  void flush() throws IOException;

  byte[] read(int size) throws IOException;

  void close();

}
