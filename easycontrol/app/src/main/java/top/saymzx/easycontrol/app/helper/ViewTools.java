package top.saymzx.easycontrol.app.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
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

import java.util.Locale;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.databinding.ItemLoadingBinding;
import top.saymzx.easycontrol.app.databinding.ItemSpinnerBinding;
import top.saymzx.easycontrol.app.databinding.ItemSwitchBinding;
import top.saymzx.easycontrol.app.databinding.ItemTextBinding;
import top.saymzx.easycontrol.app.databinding.ModuleDialogBinding;
import top.saymzx.easycontrol.app.entity.AppData;
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
  }

  // 设置语言
  public static void setLocale(Activity context) {
    Resources resources = context.getResources();
    Configuration config = resources.getConfiguration();
    String locale = AppData.setting.getLocale();
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
    // 设置异形屏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      WindowManager.LayoutParams lp = context.getWindow().getAttributes();
      lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      context.getWindow().setAttributes(lp);
    }
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

  // 创建Client加载框
  public static Pair<ItemLoadingBinding, Dialog> createLoading(Context context) {
    ItemLoadingBinding loadingView = ItemLoadingBinding.inflate(LayoutInflater.from(context));
    return new Pair<>(loadingView, createDialog(context, false, loadingView.getRoot()));
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
