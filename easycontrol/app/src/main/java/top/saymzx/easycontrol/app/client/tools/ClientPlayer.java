package top.saymzx.easycontrol.app.client.tools;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.view.Surface;

import java.nio.ByteBuffer;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.decode.AudioDecode;
import top.saymzx.easycontrol.app.client.decode.VideoDecode;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ClientPlayer {
  private boolean isClose = false;
  private final ClientController clientController;
  private final ClientStream clientStream;
  private final Thread mainStreamInThread = new Thread(this::mainStreamIn);
  private final Thread videoStreamInThread = new Thread(this::videoStreamIn);
  private Handler playHandler = null;
  private final HandlerThread playHandlerThread = new HandlerThread("easycontrol_play", Thread.MAX_PRIORITY);
  private static final int AUDIO_EVENT = 1;
  private static final int CLIPBOARD_EVENT = 2;
  private static final int CHANGE_SIZE_EVENT = 3;

  public ClientPlayer(String uuid, ClientStream clientStream) {
    clientController = Client.getClientController(uuid);
    this.clientStream = clientStream;
    if (clientController == null) return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      playHandlerThread.start();
      playHandler = new Handler(playHandlerThread.getLooper());
    }
    mainStreamInThread.start();
    videoStreamInThread.start();
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
            clientController.handleAction("setClipBoard", clientStream.readByteArrayFromMain(clientStream.readIntFromMain()), 0);
            break;
          case CHANGE_SIZE_EVENT:
            clientController.handleAction("updateVideoSize", clientStream.readByteArrayFromMain(8), 0);
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
      Pair<Integer, Integer> videoSize = new Pair<>(clientStream.readIntFromVideo(), clientStream.readIntFromVideo());
      Surface surface = new Surface(clientController.getTextureView().getSurfaceTexture());
      ByteBuffer csd0 = clientStream.readFrameFromVideo();
      ByteBuffer csd1 = useH265 ? null : clientStream.readFrameFromVideo();
      videoDecode = new VideoDecode(videoSize, surface, csd0, csd1, playHandler);
      while (!Thread.interrupted()) videoDecode.decodeIn(clientStream.readFrameFromVideo());
    } catch (Exception ignored) {
    } finally {
      if (videoDecode != null) videoDecode.release();
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
