package top.saymzx.easycontrol.app.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

import java.util.Locale;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ItemDeviceDetailBinding;
import top.saymzx.easycontrol.app.databinding.ItemLoadingBinding;
import top.saymzx.easycontrol.app.databinding.ItemSpinnerBinding;
import top.saymzx.easycontrol.app.databinding.ItemSwitchBinding;
import top.saymzx.easycontrol.app.databinding.ItemTextBinding;
import top.saymzx.easycontrol.app.databinding.ModuleDialogBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;
import top.saymzx.easycontrol.app.entity.MyInterface;

public class ViewTools {
  // 设置全面屏
  public static void setFullScreen(Activity context) {
    // 全屏显示
    context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    context.getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      WindowManager.LayoutParams lp = context.getWindow().getAttributes();
      lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      context.getWindow().setAttributes(lp);
    }
  }

  // 设置语言
  public static void setLocale(Activity context) {
    Resources resources = context.getResources();
    Configuration config = resources.getConfiguration();
    String locale = AppData.setting.getDefaultLocale();
    if (locale.equals("")) config.locale = Locale.getDefault();
    else if (locale.equals("en")) config.locale = Locale.ENGLISH;
    else if (locale.equals("zh")) config.locale = Locale.CHINESE;
    resources.updateConfiguration(config, resources.getDisplayMetrics());
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

  // 创建弹窗
  public static Dialog createDialog(Context context, boolean canCancel, View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(canCancel);
    ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
    dialogView.addView(view);
    builder.setView(dialogView);
    Dialog dialog = builder.create();
    dialog.setCanceledOnTouchOutside(canCancel);
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    return dialog;
  }

  // 创建新建设备弹窗
  public static Dialog createDeviceDetailView(
    Context context,
    Device device,
    DeviceListAdapter deviceListAdapter
  ) {
    ItemDeviceDetailBinding itemAddDeviceBinding = ItemDeviceDetailBinding.inflate(LayoutInflater.from(context));
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
  private static final String[] maxVideoBitList = new String[]{"12", "8", "4", "2", "1"};

  public static void createDeviceOptionSet(Context context, ViewGroup fatherLayout, Device device) {
    // Device为null，则视为设置默认参数
    boolean setDefault = device == null;
    // 数组适配器
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(AppData.applicationContext, R.layout.item_spinner_item, maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(AppData.applicationContext, R.layout.item_spinner_item, maxFpsList);
    ArrayAdapter<String> maxVideoBitAdapter = new ArrayAdapter<>(AppData.applicationContext, R.layout.item_spinner_item, maxVideoBitList);
    // 添加参数视图
    fatherLayout.addView(createSwitchCard(context, context.getString(R.string.option_is_audio), context.getString(R.string.option_is_audio_detail), setDefault ? AppData.setting.getDefaultIsAudio() : device.isAudio, isChecked -> {
      if (setDefault) AppData.setting.setDefaultIsAudio(isChecked);
      else device.isAudio = isChecked;
    }).getRoot());
    fatherLayout.addView(createSpinnerCard(context, context.getString(R.string.option_max_size), context.getString(R.string.option_max_size_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxSize() : device.maxSize), maxSizeAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxSize(Integer.parseInt(str));
      else device.maxSize = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(createSpinnerCard(context, context.getString(R.string.option_max_fps), context.getString(R.string.option_max_fps_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxFps() : device.maxFps), maxFpsAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxFps(Integer.parseInt(str));
      else device.maxFps = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(createSpinnerCard(context, context.getString(R.string.option_max_video_bit), context.getString(R.string.option_max_video_bit_detail), String.valueOf(setDefault ? AppData.setting.getDefaultMaxVideoBit() : device.maxVideoBit), maxVideoBitAdapter, str -> {
      if (setDefault) AppData.setting.setDefaultMaxVideoBit(Integer.parseInt(str));
      else device.maxVideoBit = Integer.parseInt(str);
    }).getRoot());
    fatherLayout.addView(createSwitchCard(context, context.getString(R.string.option_use_h265), context.getString(R.string.option_use_h265_detail), setDefault ? AppData.setting.getDefaultUseH265() : device.useH265, isChecked -> {
      if (setDefault) AppData.setting.setDefaultUseH265(isChecked);
      else device.useH265 = isChecked;
    }).getRoot());
    fatherLayout.addView(createSwitchCard(context, context.getString(R.string.option_set_resolution), context.getString(R.string.option_set_resolution_detail), setDefault ? AppData.setting.getDefaultSetResolution() : device.setResolution, isChecked -> {
      if (setDefault) AppData.setting.setDefaultSetResolution(isChecked);
      else device.setResolution = isChecked;
    }).getRoot());
  }

  // 创建Client加载框
  public static Pair<View, WindowManager.LayoutParams> createLoading(Context context) {
    ItemLoadingBinding loadingView = ItemLoadingBinding.inflate(LayoutInflater.from(context));
    WindowManager.LayoutParams loadingViewParams = new WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      PixelFormat.TRANSLUCENT
    );
    loadingViewParams.gravity = Gravity.CENTER;
    return new Pair<>(loadingView.getRoot(), loadingViewParams);
  }

  // 创建纯文本卡片
  public static ItemTextBinding createTextCard(
    Context context,
    String text,
    MyInterface.MyFunction function
  ) {
    ItemTextBinding textView = ItemTextBinding.inflate(LayoutInflater.from(context));
    textView.text.setText(text);
    if (function != null) textView.getRoot().setOnClickListener(v -> function.run());
    return textView;
  }

  // 创建开关卡片
  public static ItemSwitchBinding createSwitchCard(
    Context context,
    String text,
    String textDetail,
    boolean config,
    MyInterface.MyFunctionBoolean function
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
    MyInterface.MyFunctionString function
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

  // 更改View的形态
  public static void viewAnim(View view, boolean toShowView, int translationX, int translationY, MyInterface.MyFunctionBoolean action) {
    // 创建平移动画
    view.setTranslationX(toShowView ? translationX : 0);
    float endX = toShowView ? 0 : translationX;
    view.setTranslationY(toShowView ? translationY : 0);
    float endY = toShowView ? 0 : translationY;
    // 创建透明度动画
    view.setAlpha(toShowView ? 0f : 1f);
    float endAlpha = toShowView ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = view.animate()
      .translationX(endX)
      .translationY(endY)
      .alpha(endAlpha)
      .setDuration(toShowView ? 300 : 200)
      .setInterpolator(toShowView ? new OvershootInterpolator() : new DecelerateInterpolator());
    animator.withStartAction(() -> {
      if (action != null) action.run(true);
    });
    animator.withEndAction(() -> {
      if (action != null) action.run(false);
    });

    // 启动动画
    animator.start();
  }
}
