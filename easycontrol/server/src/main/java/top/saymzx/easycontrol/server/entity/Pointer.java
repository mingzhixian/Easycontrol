/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Pointer {

  public int id;

  public float x;

  public float y;

  public long downTime;

  public Pointer(int id, long downTime) {
    this.id = id;
    this.downTime = downTime;
  }

}
