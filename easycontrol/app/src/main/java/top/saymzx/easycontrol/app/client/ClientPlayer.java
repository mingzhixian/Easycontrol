package top.saymzx.easycontrol.app.client;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.nio.ByteBuffer;

import top.saymzx.easycontrol.app.client.decode.AudioDecode;
import top.saymzx.easycontrol.app.client.decode.VideoDecode;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientPlayer {
  private boolean isClose = false;
  private final Device device;
  private final ClientStream clientStream;
  private final ClientController clientController;
  private final Thread mainStreamInThread = new Thread(this::mainStreamIn);
  private final Thread videoStreamInThread = new Thread(this::videoStreamIn);
  private Handler playHandler = null;
  private final HandlerThread playHandlerThread = new HandlerThread("easycontrol_play", Thread.MAX_PRIORITY);
  private static final int AUDIO_EVENT = 1;
  private static final int CLIPBOARD_EVENT = 2;
  private static final int CHANGE_SIZE_EVENT = 3;

  public ClientPlayer(Device device, ClientStream clientStream, ClientController clientController) {
    this.device = device;
    this.clientStream = clientStream;
    this.clientController = clientController;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      playHandlerThread.start();
      playHandler = new Handler(playHandlerThread.getLooper());
    }
    mainStreamInThread.start();
    videoStreamInThread.start();
    AppData.uiHandler.post(this::otherService);
  }

  private void mainStreamIn() {
    AudioDecode audioDecode = null;
    boolean useOpus = true;
    try {
      if (clientStream.readByteFromMain() == 1) useOpus = clientStream.readByteFromMain() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (clientStream.readByteFromMain()) {
          case AUDIO_EVENT:
            ByteBuffer audioFrame = clientStream.readFrameFromMain();
            if (audioDecode != null) audioDecode.decodeIn(audioFrame);
            else audioDecode = new AudioDecode(useOpus, audioFrame, playHandler);
            break;
          case CLIPBOARD_EVENT:
            ClientController.handleControll(device.uuid, "setClipBoard", clientStream.readByteArrayFromMain(clientStream.readIntFromMain()));
            break;
          case CHANGE_SIZE_EVENT:
            ClientController.handleControll(device.uuid, "updateVideoSize", clientStream.readByteArrayFromMain(8));
            break;
        }
      }
    } catch (InterruptedException ignored) {
    } catch (Exception e) {
      PublicTools.logToast("player", e.toString(), false);
    } finally {
      if (audioDecode != null) audioDecode.release();
    }
  }

  private void videoStreamIn() {
    VideoDecode videoDecode = null;
    try {
      boolean useH265 = clientStream.readByteFromVideo() == 1;
      ByteBuffer csd0 = clientStream.readFrameFromVideo();
      if (useH265) videoDecode = new VideoDecode(clientController.getVideoSize(), clientController.getSurface(), csd0, null, playHandler);
      else videoDecode = new VideoDecode(clientController.getVideoSize(), clientController.getSurface(), csd0, clientStream.readFrameFromVideo(), playHandler);
      while (!Thread.interrupted()) videoDecode.decodeIn(clientStream.readFrameFromVideo());
    } catch (Exception ignored) {
    } finally {
      if (videoDecode != null) videoDecode.release();
    }
  }

  private void otherService() {
    if (!isClose) {
      ClientController.handleControll(device.uuid, "checkClipBoard", null);
      ClientController.handleControll(device.uuid, "keepAlive", null);
      ClientController.handleControll(device.uuid, "checkSizeAndSite", null);
      AppData.uiHandler.postDelayed(this::otherService, 2000);
    }
  }

  public void close() {
    if (isClose) return;
    isClose = true;
    mainStreamInThread.interrupt();
    videoStreamInThread.interrupt();
    playHandlerThread.interrupt();
  }
}
