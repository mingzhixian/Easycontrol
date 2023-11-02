package top.saymzx.easycontrol.app.entity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.hardware.usb.UsbManager;

import java.io.File;

import top.saymzx.easycontrol.adb.AdbKeyPair;
import top.saymzx.easycontrol.app.BuildConfig;
import top.saymzx.easycontrol.app.helper.DbHelper;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class AppData {
  @SuppressLint("StaticFieldLeak")
  public static Activity main;

  // 公共工具库
  public static PublicTools publicTools = new PublicTools();

  // 数据库工具
  public static DbHelper dbHelper;

  // 密钥文件
  public static AdbKeyPair keyPair;

  // 剪切板
  public static ClipboardManager clipBoard;

  // USB
  public static UsbManager usbManager;

  // 设置值
  public static Setting setting;

  // 当前版本号
  public static String serverName = "easycontrol_server_" + BuildConfig.VERSION_CODE + ".jar";

  public static void init(Activity m) {
    main = m;
    dbHelper = new DbHelper(main);
    clipBoard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
    usbManager = (UsbManager) main.getSystemService(Context.USB_SERVICE);
    setting = new Setting(main.getSharedPreferences("setting", Context.MODE_PRIVATE));
    // 读取密钥文件
    try {
      File privateKey = new File(main.getApplicationContext().getFilesDir(), "private.key");
      File publicKey = new File(main.getApplicationContext().getFilesDir(), "public.key");
      if (!privateKey.isFile() || !publicKey.isFile()) {
        AdbKeyPair.generate(privateKey, publicKey);
      }
      keyPair = AdbKeyPair.read(privateKey, publicKey);
    } catch (Exception ignored) {
    }
  }
}
