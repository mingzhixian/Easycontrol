package top.saymzx.easycontrol.app.adb;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TcpChannel implements AdbChannel {
  private final Socket socket = new Socket();
  private final InputStream inputStream;
  private final OutputStream outputStream;

  public TcpChannel(String host, int port) throws IOException {
    socket.setTcpNoDelay(true);
    socket.connect(new InetSocketAddress(host, port), 5000);
    inputStream = socket.getInputStream();
    outputStream = socket.getOutputStream();
  }

  @Override
  public void write(ByteBuffer data) throws IOException {
    outputStream.write(data.array());
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }

  @Override
  public ByteBuffer read(int size) throws IOException {
    byte[] buffer = new byte[size];
    int bytesRead = 0;
    while (bytesRead < size) {
      int bytesRemaining = size - bytesRead;
      int read = inputStream.read(buffer, bytesRead, bytesRemaining);
      if (read == -1) break;
      bytesRead += read;
    }
    return ByteBuffer.wrap(buffer);
  }

  @Override
  public void close() {
    try {
      Log.e("aaa","channel close");
      outputStream.close();
      inputStream.close();
      socket.close();
    } catch (Exception ignored) {
    }
  }
}
