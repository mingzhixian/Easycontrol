package top.saymzx.easycontrol.app.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import top.saymzx.easycontrol.adb.Adb;
import top.saymzx.easycontrol.adb.AdbStream;

public class ClientStream {
  private final int mode;
  private boolean isClosed = false;
  private final Socket socket;
  private final OutputStream outputStream;
  private final DataInputStream inputStream;
  private final Thread executeStreamOutThread = new Thread(this::executeStreamOut);

  private final Adb adb;
  private final AdbStream adbStream;

  public ClientStream(Socket socket) throws IOException {
    this.mode = 1;
    this.socket = socket;
    this.outputStream = socket.getOutputStream();
    this.inputStream = new DataInputStream(socket.getInputStream());
    this.adb = null;
    this.adbStream = null;
    executeStreamOutThread.start();
  }

  public ClientStream(Adb adb, AdbStream adbStream) {
    this.mode = 2;
    this.adb = adb;
    this.adbStream = adbStream;
    this.socket = null;
    this.outputStream = null;
    this.inputStream = null;
  }

  public byte readByte() throws IOException, InterruptedException {
    if (mode == 1) return inputStream.readByte();
    else return adbStream.readByte();
  }

  public int readInt() throws IOException, InterruptedException {
    if (mode == 1) return inputStream.readInt();
    else return adbStream.readInt();
  }

  public long readLong() throws IOException, InterruptedException {
    if (mode == 1) return inputStream.readLong();
    else return adbStream.readLong();
  }

  public byte[] readByteArray(int size) throws IOException, InterruptedException {
    if (mode == 1) {
      byte[] buffer = new byte[size];
      inputStream.readFully(buffer);
      return buffer;
    } else return adbStream.readByteArray(size).array();
  }

  public byte[] readFrame() throws IOException, InterruptedException {
    if (mode == 2) adb.sendMoreOk(adbStream);
    return readByteArray(readInt());
  }

  private final LinkedBlockingQueue<byte[]> writeBuffer = new LinkedBlockingQueue<>();

  public void write(byte[] buffer) throws IOException, InterruptedException {
    if (isClosed) throw new IOException("连接已断开");
    if (mode == 1) writeBuffer.offer(buffer);
    else adbStream.write(ByteBuffer.wrap(buffer));
  }

  private void executeStreamOut() {
    try {
      while (!Thread.interrupted()) outputStream.write(writeBuffer.take());
    } catch (Exception ignored) {
      close();
    }
  }

  public void close() {
    if (isClosed) return;
    isClosed = true;
    if (mode == 1) {
      try {
        executeStreamOutThread.interrupt();
        inputStream.close();
        outputStream.close();
        socket.close();
      } catch (Exception ignored) {
      }
    } else {
      adbStream.close();
    }
  }
}
