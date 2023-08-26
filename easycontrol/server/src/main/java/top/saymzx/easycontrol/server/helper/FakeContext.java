/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.saymzx.easycontrol.server.helper;

import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.MutableContextWrapper;
import android.os.Build;
import android.os.Process;

public final class FakeContext extends MutableContextWrapper {

  public static final String PACKAGE_NAME = "com.android.shell";
  public static final int ROOT_UID = 0; // Like android.os.Process.ROOT_UID, but before API 29

  private static final FakeContext INSTANCE = new FakeContext();

  public static FakeContext get() {
    return INSTANCE;
  }

  private FakeContext() {
    super(null);
  }

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public String getOpPackageName() {
    return PACKAGE_NAME;
  }

  @TargetApi(Build.VERSION_CODES.S)
  @Override
  public AttributionSource getAttributionSource() {
    AttributionSource.Builder builder = new AttributionSource.Builder(Process.SHELL_UID);
    builder.setPackageName(PACKAGE_NAME);
    return builder.build();
  }

  // @Override to be added on SDK upgrade for Android 14
  @SuppressWarnings("unused")
  public int getDeviceId() {
    return 0;
  }
}
