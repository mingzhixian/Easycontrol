/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.entity;

import android.view.MotionEvent;

import java.util.concurrent.ConcurrentHashMap;

public final class PointersState {

  private final int MAX_POINTERS = 10;

  private final ConcurrentHashMap<Integer, Pointer> pointers = new ConcurrentHashMap<>();
  public final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[MAX_POINTERS];
  public final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[MAX_POINTERS];

  public PointersState() {
    // 初始化指针
    for (int i = 0; i < MAX_POINTERS; ++i) {
      MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
      props.toolType = MotionEvent.TOOL_TYPE_FINGER;
      pointerProperties[i] = props;

      MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
      coords.orientation = 0;
      coords.size = 0.01f;
      coords.pressure = 1f;
      pointerCoords[i] = coords;
    }
  }

  public Pointer newPointer(int pointerId, long now) {
    for (int i = 0; i < MAX_POINTERS; i++) {
      if (isLocalIdAvailable(i)) {
        Pointer pointer = new Pointer(i, now);
        pointers.put(pointerId, pointer);
        return pointer;
      }
    }
    return null;
  }

  public Pointer get(int pointerId) {
    return pointers.get(pointerId);
  }

  private boolean isLocalIdAvailable(int localId) {
    for (Pointer value : pointers.values()) if (value.id == localId) return false;
    return true;
  }

  public void remove(int pointerId) {
    pointers.remove(pointerId);
  }

  public int update() {
    int i = 0;
    for (Pointer value : pointers.values()) {
      pointerProperties[i].id = value.id;
      pointerCoords[i].x = value.x;
      pointerCoords[i].y = value.y;
      i++;
    }
    return i;
  }

}
