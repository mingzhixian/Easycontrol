package top.saymzx.easycontrol.server.entity;

import android.util.Pair;

public final class DisplayInfo {
  public final int displayId;
  public final Pair<Integer, Integer> size;
  public final int rotation;
  public final int layerStack;
  public final int flags;

  public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 0x00000001;

  public DisplayInfo(int displayId, Pair<Integer, Integer> size, int rotation, int layerStack, int flags) {
    this.displayId = displayId;
    this.size = size;
    this.rotation = rotation;
    this.layerStack = layerStack;
    this.flags = flags;
  }
}
