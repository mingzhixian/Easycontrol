/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

public final class Pointer {

  public long id;

  public int localId;

  public float x;

  public float y;

  public boolean isUp;

  public Pointer(long id, int localId) {
    this.id = id;
    this.localId = localId;
  }
}
