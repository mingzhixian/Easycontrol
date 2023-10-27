package top.saymzx.easycontrol.app.helper;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

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
  public void setFullScreen(Activity context) {
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
  public void setStatusAndNavBar(Activity context) {
    // 导航栏
    context.getWindow().setNavigationBarColor(context.getResources().getColor(R.color.background));
    // 状态栏
    context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    context.getWindow().setStatusBarColor(context.getResources().getColor(R.color.cardContainerBackground));
    context.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }

  // DP转PX
  public int dp2px(Float dp) {
    return (int) (dp * AppData.main.getResources().getDisplayMetrics().density);
  }

  // 获取当前界面宽高
  public Pair<Integer, Integer> getScreenSize() {
    DisplayMetrics metric = new DisplayMetrics();
    AppData.main.getWindowManager().getDefaultDisplay().getRealMetrics(metric);
    return new Pair<>(metric.widthPixels, metric.heightPixels);
  }

  // 创建弹窗
  public Dialog createDialog(Context context, View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(false);
    ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
    dialogView.addView(view);
    builder.setView(dialogView);
    Dialog dialog = builder.create();
    dialog.setCanceledOnTouchOutside(true);
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    return dialog;
  }

  // 创建新建设备弹窗
  public Dialog createAddDeviceView(
    Context context,
    Device device,
    DeviceListAdapter deviceListAdapter
  ) {
    ItemAddDeviceBinding itemAddDeviceBinding = ItemAddDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = createDialog(context, itemAddDeviceBinding.getRoot());
    // 设置值
    itemAddDeviceBinding.addDeviceName.setText(device.name);
    itemAddDeviceBinding.addDeviceAddress.setText(device.address);
    // 创建View
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, maxFpsList);
    ArrayAdapter<String> videoBitAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_item, videoBitList);
    ItemSpinnerBinding maxSize = createSpinnerCard(context, "最大大小", maxSizeAdapter, device.maxSize.toString(), null);
    ItemSpinnerBinding maxFps = createSpinnerCard(context, "最大帧率", maxFpsAdapter, device.maxFps.toString(), null);
    ItemSpinnerBinding maxVideoBit = createSpinnerCard(context, "最大码率", videoBitAdapter, device.maxVideoBit.toString(), null);
    ItemSwitchBinding setResolution = createSwitchCard(context, "修改分辨率", device.setResolution, null);
    itemAddDeviceBinding.addDeviceOptions.addView(maxSize.getRoot());
    itemAddDeviceBinding.addDeviceOptions.addView(maxFps.getRoot());
    itemAddDeviceBinding.addDeviceOptions.addView(maxVideoBit.getRoot());
    itemAddDeviceBinding.addDeviceOptions.addView(setResolution.getRoot());
    // 是否显示高级选项
    itemAddDeviceBinding.addDeviceIsOptions.setOnClickListener(v -> itemAddDeviceBinding.addDeviceOptions.setVisibility(itemAddDeviceBinding.addDeviceIsOptions.isChecked() ? View.VISIBLE : View.GONE));
    // 设置确认按钮监听
    itemAddDeviceBinding.addDeviceOk.setOnClickListener(v -> {
      Device newDevice = new Device(
        device.id,
        itemAddDeviceBinding.addDeviceName.getText().toString(),
        itemAddDeviceBinding.addDeviceAddress.getText().toString(),
        Integer.parseInt(maxSize.itemSpinnerSpinner.getSelectedItem().toString()),
        Integer.parseInt(maxFps.itemSpinnerSpinner.getSelectedItem().toString()),
        Integer.parseInt(maxVideoBit.itemSpinnerSpinner.getSelectedItem().toString()),
        setResolution.itemSwitchSwitch.isChecked()
      );
      if (newDevice.id != null) AppData.dbHelper.update(newDevice);
      else AppData.dbHelper.insert(newDevice);
      deviceListAdapter.update();
      dialog.hide();
    });
    return dialog;
  }

  // 创建Client加载框
  public Dialog createClientLoading(
    Context context,
    MyFunction function
  ) {
    ItemClientLoadingBinding loadingView = ItemClientLoadingBinding.inflate(LayoutInflater.from(context));
    loadingView.text.setOnClickListener(v -> {
      if (function != null) function.run();
    });
    return createDialog(context, loadingView.getRoot());
  }

  // 创建纯文本卡片
  public ItemTextBinding createTextCard(
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
  public ItemSwitchBinding createSwitchCard(
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
  public final String[] maxSizeList = new String[]{"2560", "1920", "1600", "1280", "1024", "800"};
  public final String[] maxFpsList = new String[]{"60", "45", "25", "15"};
  public final String[] videoBitList = new String[]{"16", "12", "8", "4", "2", "1"};

  public ItemSpinnerBinding createSpinnerCard(
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

  // 更改Bar View的形态
  public void changeBarViewAnim(View view, int translationY) {
    boolean toShowBarView = view.getVisibility() == View.GONE;
    // 创建平移动画
    view.setTranslationY(toShowBarView ? translationY : 0);
    float endY = toShowBarView ? 0 : translationY;
    // 创建缩放动画
    view.setScaleY(toShowBarView ? 0f : 1f);
    float endScaleY = toShowBarView ? 1f : 0f;
    // 创建透明度动画
    view.setAlpha(toShowBarView ? 0f : 1f);
    float endAlpha = toShowBarView ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = view.animate()
      .translationY(endY)
      .scaleY(endScaleY)
      .alpha(endAlpha)
      .setDuration(toShowBarView ? 200 : 250)
      .setInterpolator(toShowBarView ? new DecelerateInterpolator() : new OvershootInterpolator());

    // 显示或隐藏
    animator.setListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {
        if (toShowBarView) view.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        if (!toShowBarView) view.setVisibility(View.GONE);
      }

      @Override
      public void onAnimationCancel(Animator animation) {
      }

      @Override
      public void onAnimationRepeat(Animator animation) {
      }
    });

    // 启动动画
    animator.start();
  }

  // 分离地址和端口号
  public Pair<String, Integer> getIpAndPort(String address) {
    String pattern;
    // ipv6
    if (address.contains("[")) {
      pattern = "(\\[.*?\\]):(\\d+)";
    }
    // 域名
    else if (Pattern.matches(".*[a-zA-Z].*", address)) {
      pattern = "(.*?):(\\d+)";
    } else {
      pattern = "(.*?):(\\d+)";
    }
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(address);
    if (matcher.find()) {
      final String[] ip = {matcher.group(1)};
      int port = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
      return new Pair<>(ip[0], port);
    }
    return null;
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