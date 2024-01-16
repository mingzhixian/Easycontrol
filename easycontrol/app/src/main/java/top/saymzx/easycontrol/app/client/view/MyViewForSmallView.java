package top.saymzx.easycontrol.app.client.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyViewForSmallView extends FrameLayout {

  private MyFunctionMotionEvent onTouchHandle;

  public MyViewForSmallView(@NonNull Context context) {
    super(context);
  }

  public MyViewForSmallView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public MyViewForSmallView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public MyViewForSmallView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (onTouchHandle != null) onTouchHandle.run(ev);
    return super.dispatchTouchEvent(ev);
  }

  public void setOnTouchHandle(MyFunctionMotionEvent handle) {
    onTouchHandle = handle;
  }

  public interface MyFunctionMotionEvent {
    void run(MotionEvent event);
  }
}
