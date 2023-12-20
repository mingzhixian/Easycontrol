package top.saymzx.easycontrol.adb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpChannel implements AdbChannel {
  private final Socket socket = new Socket();
  private final InputStream inputStream;
  private final OutputStream outputStream;

  public TcpChannel(String host, int port) throws IOException {
    socket.setTcpNoDelay(true);
    socket.connect(new InetSocketAddress(host, port), 2000);
    inputStream = socket.getInputStream();
    outputStream = socket.getOutputStream();
  }

  @Override
  public void write(byte[] data) throws IOException {
    outputStream.write(data);
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }

  @Override
  public byte[] read(int size) throws IOException {
    byte[] buffer = new byte[size];
    int bytesRead = 0;
    while (bytesRead < size) {
      int bytesRemaining = size - bytesRead;
      int read = inputStream.read(buffer, bytesRead, bytesRemaining);
      if (read == -1) break;
      bytesRead += read;
    }
    return buffer;
  }

  @Override
  public void close() {
    try {
      outputStream.close();
      inputStream.close();
      socket.close();
    } catch (Exception ignored) {
    }
  }
}
