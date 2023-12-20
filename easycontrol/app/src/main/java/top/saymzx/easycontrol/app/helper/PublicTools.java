package top.saymzx.easycontrol.app.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.DhcpInfo;
import android.os.Build;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ItemAddDeviceBinding;
import top.saymzx.easycontrol.app.databinding.ItemLoadingBinding;
import top.saymzx.easycontrol.app.databinding.ItemSpinnerBinding;
import top.saymzx.easycontrol.app.databinding.ItemSwitchBinding;
import top.saymzx.easycontrol.app.databinding.ItemTextBinding;
import top.saymzx.easycontrol.app.databinding.ModuleDialogBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class PublicTools {

  // 设置全面屏
  public static void setFullScreen(Activity context) {
    // 全屏显示
    context.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
      View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
      View.SYSTEM_UI_FLAG_FULLSCREEN |
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) context.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
  }

  // 设置状态栏导航栏颜色
  public static void setStatusAndNavBar(Activity context) {
    // 导航栏
    context.getWindow().setNavigationBarColor(context.getResources().getColor(R.color.background));
    // 状态栏
    context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    context.getWindow().setStatusBarColor(context.getResources().getColor(R.color.background));
    if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES)
      context.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }

  // DP转PX
  public static int dp2px(Float dp) {
    return (int) (dp * AppData.realScreenSize.density);
  }

  // 创建弹窗
  public static Dialog createDialog(Context context, boolean canCancel, View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(true);
    ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
    dialogView.addView(view);
    builder.setView(dialogView);
    Dialog dialog = builder.create();
    dialog.setCanceledOnTouchOutside(canCancel);
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    return dialog;
  }

  // 创建新建设备弹窗
  public static Dialog createAddDeviceView(
    Context context,
    Device device,
    DeviceListAdapter deviceListAdapter
  ) {
    ItemAddDeviceBinding itemAddDeviceBinding = ItemAddDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = createDialog(context, true, itemAddDeviceBinding.getRoot());
    // 设置值
    itemAddDeviceBinding.name.setText(device.name);
    itemAddDeviceBinding.address.setText(device.address);
    // 创建View
    createDeviceOptionSet(context, itemAddDeviceBinding.options, device);
    // 特殊设备不允许修改
    if (!device.isNormalDevice()) itemAddDeviceBinding.address.setEnabled(false);
    // 是否显示高级选项
    itemAddDeviceBinding.isOptions.setOnClickListener(v -> itemAddDeviceBinding.options.setVisibility(itemAddDeviceBinding.isOptions.isChecked() ? View.VISIBLE : View.GONE));
    // 设置确认按钮监听
    itemAddDeviceBinding.ok.setOnClickListener(v -> {
      if (device.type == Device.TYPE_NORMAL && String.valueOf(itemAddDeviceBinding.address.getText()).equals("")) return;
      device.name = String.valueOf(itemAddDeviceBinding.name.getText());
      device.address = String.valueOf(itemAddDeviceBinding.address.getText());
      if (AppData.dbHelper.getByUUID(device.uuid) != null) AppData.dbHelper.update(device);
      else AppData.dbHelper.insert(device);
      deviceListAdapter.update();
      dialog.cancel();
    });
    return dialog;
  }

  // 创建设备参数设置页面
  private static final String[] maxSizeList = new String[]{"2560", "1920", "1600", "1280", "1024", "800"};
  private static final String[] maxFpsList = new String[]{"90", "60", "40", "30", "20", "10"};
  private static final String[] maxVideoBitList = new String[]{"16", "12", "8", "4", "2", "1"};

  public static void createDeviceOptionSet(Context context, ViewGroup fatherLayout, Device device) {
    // Device为null，则视为设置默认参数
    boolean setDefault = device == null;
    // 数组适配器
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(AppData.main, R.layout.item_spinner_item, maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(AppData.main, R.layout.item_spinner_item, maxFpsList);
    ArrayAdapter<String> maxVideoBitAdapter = new ArrayAdapter<>(AppData.main, R.layout.item_spinner_item, maxVideoBitList);
    // 添加参数视图
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_is_audio), context.getString(R.string.option_is_audio_detail), setDefault ? AppData.setting.getDefaultIsAudio() : device.isAudio, isChecked -> {
      if (setDefault) AppData.setting.setDefaultIsAudio(isChecked);
      else device.isAudio = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSpinnerCard(context, context.getString(R.string.option_max_size), context.getString(R.string.option_max_size_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxSize() : device.maxSize), maxSizeAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxSize(Integer.parseInt(str));
      else device.maxSize = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(PublicTools.createSpinnerCard(context, context.getString(R.string.option_max_fps), context.getString(R.string.option_max_fps_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxFps() : device.maxFps), maxFpsAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxFps(Integer.parseInt(str));
      else device.maxFps = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(PublicTools.createSpinnerCard(context, context.getString(R.string.option_max_video_bit), context.getString(R.string.option_max_video_bit_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxVideoBit() : device.maxVideoBit), maxVideoBitAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxVideoBit(Integer.parseInt(str));
      else device.maxVideoBit = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_use_h265), context.getString(R.string.option_use_h265_detail), setDefault ? AppData.setting.getDefaultUseH265() : device.useH265, isChecked -> {
      if (setDefault) AppData.setting.setDefaultUseH265(isChecked);
      else device.useH265 = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_use_opus), context.getString(R.string.option_use_opus_detail), setDefault ? AppData.setting.getDefaultUseOpus() : device.useOpus, isChecked -> {
      if (setDefault) AppData.setting.setDefaultUseOpus(isChecked);
      else device.useOpus = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_turn_off_screen), context.getString(R.string.option_turn_off_screen_detail), setDefault ? AppData.setting.getDefaultTurnOffScreen() : device.turnOffScreen, isChecked -> {
      if (setDefault) AppData.setting.setDefaultTurnOffScreen(isChecked);
      else device.turnOffScreen = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_auto_lock_after_control), context.getString(R.string.option_auto_lock_after_control_detail), setDefault ? AppData.setting.getDefaultAutoLockAfterControl() : device.autoLockAfterControl, isChecked -> {
      if (setDefault) AppData.setting.setDefaultAutoLockAfterControl(isChecked);
      else device.autoLockAfterControl = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_default_full), context.getString(R.string.option_default_full_detail), setDefault ? AppData.setting.getDefaultFull() : device.defaultFull, isChecked -> {
      if (setDefault) AppData.setting.setDefaultFull(isChecked);
      else device.defaultFull = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_set_resolution), context.getString(R.string.option_set_resolution_detail), setDefault ? AppData.setting.getDefaultSetResolution() : device.setResolution, isChecked -> {
      if (setDefault) AppData.setting.setDefaultSetResolution(isChecked);
      else device.setResolution = isChecked;
    }).getRoot());
    fatherLayout.addView(PublicTools.createSwitchCard(context, context.getString(R.string.option_use_tunnel), context.getString(R.string.option_use_tunnel_detail), setDefault ? AppData.setting.getDefaultUseTunnel() : device.useTunnel, isChecked -> {
      if (setDefault) AppData.setting.setDefaultUseTunnel(isChecked);
      else device.useTunnel = isChecked;
    }).getRoot());
  }

  // 创建Client加载框
  public static Dialog createClientLoading(
    Context context
  ) {
    ItemLoadingBinding loadingView = ItemLoadingBinding.inflate(LayoutInflater.from(context));
    return createDialog(context, false, loadingView.getRoot());
  }

  // 创建纯文本卡片
  public static ItemTextBinding createTextCard(
    Context context,
    String text,
    MyFunction function
  ) {
    ItemTextBinding textView = ItemTextBinding.inflate(LayoutInflater.from(context));
    textView.getRoot().setText(text);
    if (function != null) textView.getRoot().setOnClickListener(v -> function.run());
    return textView;
  }

  // 创建开关卡片
  public static ItemSwitchBinding createSwitchCard(
    Context context,
    String text,
    String textDetail,
    boolean config,
    MyFunctionBoolean function
  ) {
    ItemSwitchBinding switchView = ItemSwitchBinding.inflate(LayoutInflater.from(context));
    switchView.itemText.setText(text);
    switchView.itemDetail.setText(textDetail);
    switchView.itemSwitch.setChecked(config);
    if (function != null) switchView.itemSwitch.setOnCheckedChangeListener((buttonView, checked) -> function.run(checked));
    return switchView;
  }

  // 创建列表卡片
  public static ItemSpinnerBinding createSpinnerCard(
    Context context,
    String text,
    String textDetail,
    String config,
    ArrayAdapter<String> adapter,
    MyFunctionString function
  ) {
    ItemSpinnerBinding spinnerView = ItemSpinnerBinding.inflate(LayoutInflater.from(context));
    spinnerView.itemText.setText(text);
    spinnerView.itemDetail.setText(textDetail);
    spinnerView.itemSpinner.setAdapter(adapter);
    spinnerView.itemSpinner.setSelection(adapter.getPosition(config));
    spinnerView.itemSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (function != null)
          function.run(spinnerView.itemSpinner.getSelectedItem().toString());
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    return spinnerView;
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
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(address);
    if (!matcher.find()) throw new IOException(AppData.main.getString(R.string.error_address_error));
    String ip = matcher.group(1);
    String port = matcher.group(2);
    if (ip == null || port == null) throw new IOException(AppData.main.getString(R.string.error_address_error));
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
    return decodeIntToIp(AppData.wifiManager.getDhcpInfo().gateway, 4);
  }

  // 获取子网地址
  public static String getNetAddress() {
    DhcpInfo dhcpInfo = AppData.wifiManager.getDhcpInfo();
    int gateway = dhcpInfo.gateway;
    int ipAddress = dhcpInfo.ipAddress;
    // 因为dhcpInfo.netmask兼容性不好，部分设备获取值为0，所以此处使用对比方法
    int len;
    if (((gateway >> 8) & 0xff) == ((ipAddress >> 8) & 0xff)) len = 3;
    else if (((gateway >> 16) & 0xff) == ((ipAddress >> 16) & 0xff)) len = 2;
    else len = 1;
    return decodeIntToIp(gateway, len);
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

  // 获取解码器是否支持
  public static boolean isDecoderSupport(String mimeName) {
    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) if (!mediaCodecInfo.isEncoder() && mediaCodecInfo.getName().contains(mimeName)) return true;
    return false;
  }

  public interface MyFunction {
    void run();
  }

  public interface MyFunctionBoolean {
    void run(Boolean bool);
  }

  public interface MyFunctionString {
    void run(String str);
  }

  public interface MyFunctionBytes {
    void run(byte[] buffer);
  }

}