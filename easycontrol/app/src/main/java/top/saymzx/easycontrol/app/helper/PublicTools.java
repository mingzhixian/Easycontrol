package top.saymzx.easycontrol.app.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ItemAddDeviceBinding;
import top.saymzx.easycontrol.app.databinding.ItemClientLoadingBinding;
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      context.getWindow().getAttributes().layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
    }
  }

  // 设置状态栏导航栏颜色
  public static void setStatusAndNavBar(Activity context) {
    // 导航栏
    context.getWindow().setNavigationBarColor(context.getResources().getColor(R.color.background));
    // 状态栏
    context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    context.getWindow().setStatusBarColor(context.getResources().getColor(R.color.cardContainerBackground));
    if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES) {
      context.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
  }

  // DP转PX
  public static int dp2px(Float dp) {
    return (int) (dp * AppData.main.getResources().getDisplayMetrics().density);
  }

  // 获取当前界面宽高
  public static Pair<Integer, Integer> getScreenSize() {
    DisplayMetrics metric = new DisplayMetrics();
    AppData.main.getWindowManager().getDefaultDisplay().getRealMetrics(metric);
    return new Pair<>(metric.widthPixels, metric.heightPixels);
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
    itemAddDeviceBinding.isAudio.setChecked(device.isAudio);
    // 创建View
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, maxFpsList);
    ArrayAdapter<String> videoBitAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, videoBitList);
    ItemSpinnerBinding maxSize = createSpinnerCard(context, "最大大小", maxSizeAdapter, device.maxSize.toString(), null);
    ItemSpinnerBinding maxFps = createSpinnerCard(context, "最大帧率", maxFpsAdapter, device.maxFps.toString(), null);
    ItemSpinnerBinding maxVideoBit = createSpinnerCard(context, "最大码率", videoBitAdapter, device.maxVideoBit.toString(), null);
    ItemSwitchBinding setResolution = createSwitchCard(context, "修改分辨率", device.setResolution, null);
    ItemSwitchBinding turnOffScreen = createSwitchCard(context, "熄屏控制", device.turnOffScreen, null);
    ItemSwitchBinding autoControlScreen = createSwitchCard(context, "自动屏幕控制", device.autoControlScreen, null);
    ItemSwitchBinding defaultFull = createSwitchCard(context, "默认全屏", device.defaultFull, null);
    itemAddDeviceBinding.options.addView(maxSize.getRoot());
    itemAddDeviceBinding.options.addView(maxFps.getRoot());
    itemAddDeviceBinding.options.addView(maxVideoBit.getRoot());
    itemAddDeviceBinding.options.addView(setResolution.getRoot());
    itemAddDeviceBinding.options.addView(turnOffScreen.getRoot());
    itemAddDeviceBinding.options.addView(autoControlScreen.getRoot());
    itemAddDeviceBinding.options.addView(defaultFull.getRoot());
    // 特殊设备不允许修改
    if (!device.isNormalDevice()) {
      itemAddDeviceBinding.name.setEnabled(false);
      itemAddDeviceBinding.address.setEnabled(false);
    }
    // 是否显示高级选项
    itemAddDeviceBinding.isOptions.setOnClickListener(v -> itemAddDeviceBinding.options.setVisibility(itemAddDeviceBinding.isOptions.isChecked() ? View.VISIBLE : View.GONE));
    // 设置确认按钮监听
    itemAddDeviceBinding.ok.setOnClickListener(v -> {
      Device newDevice = new Device(
        device.id, device.type,
        String.valueOf(itemAddDeviceBinding.name.getText()),
        String.valueOf(itemAddDeviceBinding.address.getText()),
        itemAddDeviceBinding.isAudio.isChecked(),
        Integer.parseInt(String.valueOf(maxSize.itemSpinnerSpinner.getSelectedItem())),
        Integer.parseInt(String.valueOf(maxFps.itemSpinnerSpinner.getSelectedItem())),
        Integer.parseInt(String.valueOf(maxVideoBit.itemSpinnerSpinner.getSelectedItem())),
        setResolution.itemSwitchSwitch.isChecked(),
        turnOffScreen.itemSwitchSwitch.isChecked(),
        autoControlScreen.itemSwitchSwitch.isChecked(),
        defaultFull.itemSwitchSwitch.isChecked()
      );
      if (newDevice.id != null) AppData.dbHelper.update(newDevice);
      else AppData.dbHelper.insert(newDevice);
      deviceListAdapter.update();
      dialog.cancel();
    });
    return dialog;
  }

  // 创建Client加载框
  public static Dialog createClientLoading(
    Context context,
    MyFunction function
  ) {
    ItemClientLoadingBinding loadingView = ItemClientLoadingBinding.inflate(LayoutInflater.from(context));
    loadingView.text.setOnClickListener(v -> {
      if (function != null) function.run();
    });
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
    textView.getRoot().setOnClickListener(v -> {
      if (function != null) function.run();
    });
    return textView;
  }

  // 创建开关卡片
  public static ItemSwitchBinding createSwitchCard(
    Context context,
    String text,
    Boolean isChecked,
    MyFunctionBoolean function
  ) {
    ItemSwitchBinding switchView = ItemSwitchBinding.inflate(LayoutInflater.from(context));
    switchView.itemSwitchText.setText(text);
    switchView.itemSwitchSwitch.setChecked(isChecked);
    switchView.itemSwitchSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
      if (function != null) function.run(checked);
    });
    return switchView;
  }

  // 创建列表卡片
  public static final String[] maxSizeList = new String[]{"2560", "1920", "1600", "1280", "1024", "800"};
  public static final String[] maxFpsList = new String[]{"60", "45", "35", "25", "15"};
  public static final String[] videoBitList = new String[]{"16", "12", "8", "4", "2", "1"};

  public static ItemSpinnerBinding createSpinnerCard(
    Context context,
    String text,
    ArrayAdapter<String> adapter,
    String defaultText,
    MyFunctionString function
  ) {
    ItemSpinnerBinding spinnerView = ItemSpinnerBinding.inflate(LayoutInflater.from(context));
    spinnerView.itemSpinnerText.setText(text);
    spinnerView.itemSpinnerSpinner.setAdapter(adapter);
    spinnerView.itemSpinnerSpinner.setSelection(adapter.getPosition(defaultText));
    spinnerView.itemSpinnerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (function != null)
          function.run(spinnerView.itemSpinnerSpinner.getSelectedItem().toString());
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    return spinnerView;
  }

  // 分离地址和端口号
  public static Pair<String, Integer> getIpAndPort(String address) {
    String pattern;
    // ipv6
    if (address.contains("[")) pattern = "(\\[.*?]):(\\d+)";
      // 域名
    else if (Pattern.matches(".*[a-zA-Z].*", address)) pattern = "(.*?):(\\d+)";
    else pattern = "(.*?):(\\d+)";
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(address);
    if (matcher.find()) {
      final String[] ip = {matcher.group(1)};
      int port = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
      return new Pair<>(ip[0], port);
    }
    return null;
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
          if (!inetAddress.isLoopbackAddress())
            if (inetAddress instanceof Inet4Address)
              ipv4Addresses.add(inetAddress.getHostAddress());
            else if (inetAddress instanceof Inet6Address && !inetAddress.isLinkLocalAddress())
              ipv6Addresses.add(inetAddress.getHostAddress());
        }
      }
    } catch (Exception ignored) {
    }
    return new Pair<>(ipv4Addresses, ipv6Addresses);
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

}