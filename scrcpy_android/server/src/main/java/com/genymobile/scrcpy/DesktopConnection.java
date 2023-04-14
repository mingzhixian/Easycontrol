package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class DesktopConnection implements Closeable {

  private static final int DEVICE_NAME_FIELD_LENGTH = 64;

  private static final String SOCKET_NAME_PREFIX = "scrcpy";

  private final Socket videoSocket;
  private final FileDescriptor videoFd;

  private final Socket audioSocket;
  private final FileDescriptor audioFd;

  private final Socket controlSocket;
  private final InputStream controlInputStream;
  private final OutputStream controlOutputStream;

  private final ControlMessageReader reader = new ControlMessageReader();
  private final DeviceMessageWriter writer = new DeviceMessageWriter();

  private DesktopConnection(Socket videoSocket, Socket audioSocket, Socket controlSocket) throws IOException {
    this.videoSocket = videoSocket;
    this.controlSocket = controlSocket;
    this.audioSocket = audioSocket;
    if (controlSocket != null) {
      controlInputStream = controlSocket.getInputStream();
      controlOutputStream = controlSocket.getOutputStream();
    } else {
      controlInputStream = null;
      controlOutputStream = null;
    }
    videoFd = ((FileOutputStream) videoSocket.getOutputStream()).getFD();
    audioFd = audioSocket != null ? ((FileOutputStream) audioSocket.getOutputStream()).getFD() : null;
  }

  private static LocalSocket connect(String abstractName) throws IOException {
    LocalSocket localSocket = new LocalSocket();
    localSocket.connect(new LocalSocketAddress(abstractName));
    return localSocket;
  }

  private static String getSocketName(int scid) {
    if (scid == -1) {
      // If no SCID is set, use "scrcpy" to simplify using scrcpy-server alone
      return SOCKET_NAME_PREFIX;
    }

    return SOCKET_NAME_PREFIX + String.format("_%08x", scid);
  }

  public static DesktopConnection open(int scid, boolean tunnelForward, boolean audio, boolean control, boolean sendDummyByte) throws IOException {
    Socket videoSocket = null;
    Socket audioSocket = null;
    Socket controlSocket = null;
    try {
      if (tunnelForward) {
        try (ServerSocket localServerSocket = new ServerSocket(6006)) {
          videoSocket = localServerSocket.accept();
          if (sendDummyByte) {
            // send one byte so the client may read() to detect a connection error
            videoSocket.getOutputStream().write(0);
          }
          if (audio) {
            audioSocket = localServerSocket.accept();
          }
          if (control) {
            controlSocket = localServerSocket.accept();
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      if (videoSocket != null) {
        videoSocket.close();
      }
      if (audioSocket != null) {
        audioSocket.close();
      }
      if (controlSocket != null) {
        controlSocket.close();
      }
      throw e;
    }

    return new DesktopConnection(videoSocket, audioSocket, controlSocket);
  }

  public void close() throws IOException {
    videoSocket.shutdownInput();
    videoSocket.shutdownOutput();
    videoSocket.close();
    if (audioSocket != null) {
      audioSocket.shutdownInput();
      audioSocket.shutdownOutput();
      audioSocket.close();
    }
    if (controlSocket != null) {
      controlSocket.shutdownInput();
      controlSocket.shutdownOutput();
      controlSocket.close();
    }
  }

  public void sendDeviceMeta(String deviceName) throws IOException {
    byte[] buffer = new byte[DEVICE_NAME_FIELD_LENGTH];

    byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
    int len = StringUtils.getUtf8TruncationIndex(deviceNameBytes, DEVICE_NAME_FIELD_LENGTH - 1);
    System.arraycopy(deviceNameBytes, 0, buffer, 0, len);
    // byte[] are always 0-initialized in java, no need to set '\0' explicitly

    IO.writeFully(videoFd, buffer, 0, buffer.length);
  }

  public FileDescriptor getVideoFd() {
    return videoFd;
  }

  public FileDescriptor getAudioFd() {
    return audioFd;
  }

  public ControlMessage receiveControlMessage() throws IOException {
    ControlMessage msg = reader.next();
    while (msg == null) {
      reader.readFrom(controlInputStream);
      msg = reader.next();
    }
    return msg;
  }

  public void sendDeviceMessage(DeviceMessage msg) throws IOException {
    writer.writeTo(msg, controlOutputStream);
  }
}
