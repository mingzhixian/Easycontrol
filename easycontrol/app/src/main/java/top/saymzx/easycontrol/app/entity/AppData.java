package top.saymzx.easycontrol.app.entity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;

import top.saymzx.easycontrol.adb.AdbKeyPair;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.helper.DbHelper;

public class AppData {
  @SuppressLint("StaticFieldLeak")
  public static Context main;
  public static Handler handler;

  // 数据库工具库
  public static DbHelper dbHelper;

  // 密钥文件
  public static AdbKeyPair keyPair;

  // 剪切板
  public static ClipboardManager clipBoard;

  // Wifi
  public static WifiManager wifiManager;

  // USB
  public static UsbManager usbManager;

  // 窗口管理
  public static WindowManager windowManager;

  // 设置值
  public static Setting setting;

  // 系统分辨率
  public static final DisplayMetrics realScreenSize = new DisplayMetrics();

  // 当前版本号
  public static String serverName = "/data/local/tmp/easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";

  public static void init(Activity m) {
    main = m;
    handler = new android.os.Handler(m.getMainLooper());
    dbHelper = new DbHelper(main);
    clipBoard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
    wifiManager = (WifiManager) main.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    usbManager = (UsbManager) main.getSystemService(Context.USB_SERVICE);
    windowManager = (WindowManager) main.getSystemService(Context.WINDOW_SERVICE);
    getRealScreenSize(m);
    setting = new Setting(main.getSharedPreferences("setting", Context.MODE_PRIVATE));
    // 读取密钥文件
    try {
      File privateKey = new File(main.getApplicationContext().getFilesDir(), "private.key");
      File publicKey = new File(main.getApplicationContext().getFilesDir(), "public.key");
      if (!privateKey.isFile() || !publicKey.isFile()) AdbKeyPair.generate(privateKey, publicKey);
      keyPair = AdbKeyPair.read(privateKey, publicKey);
    } catch (Exception ignored) {
    }
  }

  // 获取设备真实分辨率
  private static void getRealScreenSize(Activity m) {
    Display display = m.getWindowManager().getDefaultDisplay();
    display.getRealMetrics(realScreenSize);
    int rotation = display.getRotation();
    if (rotation == 1 || rotation == 3) {
      int tmp = realScreenSize.heightPixels;
      realScreenSize.heightPixels = realScreenSize.widthPixels;
      realScreenSize.widthPixels = tmp;
    }
  }
}
