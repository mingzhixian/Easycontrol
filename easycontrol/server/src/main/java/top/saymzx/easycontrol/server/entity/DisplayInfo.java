/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

import android.util.Pair;

public final class DisplayInfo {
  public final int displayId;
  public final int width;
  public final int height;
  public final int rotation;
  public final int layerStack;
  public final int density;

  public DisplayInfo(int displayId, int width,int height, int rotation,int density, int layerStack) {
    this.displayId = displayId;
    this.width=width;
    this.height=height;
    this.rotation = rotation;
    this.layerStack = layerStack;
    this.density = density;
  }
}
