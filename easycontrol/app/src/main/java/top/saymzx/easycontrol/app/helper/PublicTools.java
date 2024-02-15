package top.saymzx.easycontrol.app.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.adb.AdbBase64;
import top.saymzx.easycontrol.app.adb.AdbKeyPair;
import top.saymzx.easycontrol.app.entity.AppData;

public class PublicTools {

  // DP转PX
  public static int dp2px(Float dp) {
    return (int) (dp * getScreenSize().density);
  }

  // 分离地址和端口号
  public static Pair<String, Integer> getIpAndPort(String address) throws IOException {
    String pattern;
    int type;
    // 特殊格式
    if (address.contains("*")) {
      type = 2;
      pattern = "(\\*.*?\\*.*):(\\d+)";
    }
    // IPv6
    else if (address.contains("[")) {
      type = 6;
      pattern = "(\\[.*?]):(\\d+)";
    }
    // 域名
    else if (Pattern.matches(".*[a-zA-Z].*", address)) {
      type = 1;
      pattern = "(.*?):(\\d+)";
    }
    // IPv4
    else {
      type = 4;
      pattern = "(.*?):(\\d+)";
    }
    Matcher matcher = Pattern.compile(pattern).matcher(address);
    if (!matcher.find()) throw new IOException(AppData.applicationContext.getString(R.string.error_address_error));
    String ip = matcher.group(1);
    String port = matcher.group(2);
    if (ip == null || port == null) throw new IOException(AppData.applicationContext.getString(R.string.error_address_error));
    // 特殊格式
    if (type == 2) {
      if (ip.equals("*gateway*")) ip = getGateway();
      if (ip.contains("*netAddress*")) ip = ip.replace("*netAddress*", getNetAddress());
    }
    // 域名解析
    else if (type == 1) {
      ip = InetAddress.getByName(ip).getHostAddress();
    }
    return new Pair<>(ip, Integer.parseInt(port));
  }

  // 获取IP地址
  public static Pair<ArrayList<String>, ArrayList<String>> getIp() {
    ArrayList<String> ipv4Addresses = new ArrayList<>();
    ArrayList<String> ipv6Addresses = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
          InetAddress inetAddress = inetAddresses.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            if (inetAddress instanceof Inet4Address) ipv4Addresses.add(inetAddress.getHostAddress());
            else if (inetAddress instanceof Inet6Address && !inetAddress.isLinkLocalAddress()) ipv6Addresses.add("[" + inetAddress.getHostAddress() + "]");
          }
        }
      }
    } catch (Exception ignored) {
    }
    return new Pair<>(ipv4Addresses, ipv6Addresses);
  }

  // 获取网关地址
  public static String getGateway() {
    int ip = AppData.wifiManager.getDhcpInfo().gateway;
    // 没有wifi时，设置为1.1.1.1
    if (ip == 0) ip = 16843009;
    return decodeIntToIp(ip, 4);
  }

  // 获取子网地址
  public static String getNetAddress() {
    // 因为此标识符使用场景有限，为了节省资源，默认地址为24位掩码地址
    return decodeIntToIp(AppData.wifiManager.getDhcpInfo().gateway, 3);
  }

  // 解析地址
  private static String decodeIntToIp(int ip, int len) {
    if (len < 1 || len > 4) return "";
    StringBuilder builder = new StringBuilder();
    builder.append(ip & 0xff);
    if (len > 1) {
      builder.append(".");
      builder.append((ip >> 8) & 0xff);
      if (len > 2) {
        builder.append(".");
        builder.append((ip >> 16) & 0xff);
        if (len > 3) {
          builder.append(".");
          builder.append((ip >> 24) & 0xff);
        }
      }
    }
    return builder.toString();
  }

  // 浏览器打开
  public static void startUrl(Context context, String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.addCategory(Intent.CATEGORY_BROWSABLE);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setData(Uri.parse(url));
      context.startActivity(intent);
    } catch (Exception ignored) {
      Toast.makeText(context, context.getString(R.string.error_no_browser), Toast.LENGTH_SHORT).show();
    }
  }

  // 日志
  public static void logToast(String type, String msg, boolean showToast) {
    Log.e("Easycontrol_" + type, msg);
    if (showToast) AppData.uiHandler.post(() -> Toast.makeText(AppData.applicationContext, type + ":" + msg, Toast.LENGTH_SHORT).show());
  }

  // 获取密钥文件
  public static Pair<File, File> getAdbKeyFile(Context context) {
    return new Pair<>(new File(context.getApplicationContext().getFilesDir(), "public.key"), new File(context.getApplicationContext().getFilesDir(), "private.key"));
  }

  // 读取密钥
  public static AdbKeyPair readAdbKeyPair() {
    try {
      AdbKeyPair.setAdbBase64(new AdbBase64() {
        @Override
        public String encodeToString(byte[] data) {
          return Base64.encodeToString(data, Base64.DEFAULT);
        }

        @Override
        public byte[] decode(byte[] data) {
          return Base64.decode(data, Base64.DEFAULT);
        }
      });
      Pair<File, File> adbKeyFile = PublicTools.getAdbKeyFile(AppData.applicationContext);
      if (!adbKeyFile.first.isFile() || !adbKeyFile.second.isFile()) AdbKeyPair.generate(adbKeyFile.first, adbKeyFile.second);
      return AdbKeyPair.read(adbKeyFile.first, adbKeyFile.second);
    } catch (Exception ignored) {
      return reGenerateAdbKeyPair();
    }
  }

  // 生成密钥
  public static AdbKeyPair reGenerateAdbKeyPair() {
    try {
      Pair<File, File> adbKeyFile = PublicTools.getAdbKeyFile(AppData.applicationContext);
      AdbKeyPair.generate(adbKeyFile.first, adbKeyFile.second);
      return AdbKeyPair.read(adbKeyFile.first, adbKeyFile.second);
    } catch (Exception ignored) {
      return null;
    }
  }

  // 获取设备当前分辨率
  public static DisplayMetrics getScreenSize() {
    DisplayMetrics screenSize = new DisplayMetrics();
    Display display = AppData.windowManager.getDefaultDisplay();
    display.getRealMetrics(screenSize);
    return screenSize;
  }

  // 扫描局域网设备
  public static ArrayList<String> scanAddress() {
    ArrayList<String> scannedAddresses = new ArrayList<>();
    String subnet = getNetAddress();
    ExecutorService executor = Executors.newFixedThreadPool(128);
    for (int i = 1; i <= 255; i++) {
      String host = subnet + "." + i;
      executor.execute(() -> {
        try {
          Socket socket = new Socket();
          socket.connect(new InetSocketAddress(host, 5555), 1000);
          socket.close();
          scannedAddresses.add(host + ":5555");
        } catch (Exception ignored) {
        }
      });
    }
    executor.shutdown();
    try {
      while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
      }
    } catch (InterruptedException ignored) {
    }
    return scannedAddresses;
  }

}