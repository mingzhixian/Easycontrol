package top.saymzx.easycontrol.server.entity;

public final class Pointer {

  public long id;

  public int localId;

  public float x;

  public float y;

  public float pressure;

  public boolean isUp;

  public Pointer(long id, int localId) {
    this.id = id;
    this.localId = localId;
  }
}
