package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.File;

import top.saymzx.easycontrol.adb.AdbKeyPair;
import top.saymzx.easycontrol.app.databinding.ActivitySetBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setActivity = ActivitySetBinding.inflate(this.getLayoutInflater());
    setContentView(setActivity.getRoot());
    // 设置状态栏导航栏颜色沉浸
    PublicTools.setStatusAndNavBar(this);
    // 设置页面
    drawUi();
    setButtonListener();
  }

  // 设置默认值
  private void drawUi() {
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, PublicTools.maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, PublicTools.maxFpsList);
    ArrayAdapter<String> videoBitAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, PublicTools.videoBitList);
    // 默认配置
    setActivity.setDefault.addView(PublicTools.createSwitchCard(this, "使能音频", AppData.setting.getDefaultIsAudio(), isChecked -> AppData.setting.setDefaultIsAudio(isChecked)).getRoot());
    setActivity.setDefault.addView(PublicTools.createSpinnerCard(this, "最大大小", maxSizeAdapter, String.valueOf(AppData.setting.getDefaultMaxSize()), str -> AppData.setting.setDefaultMaxSize(Integer.parseInt(str))).getRoot());
    setActivity.setDefault.addView(PublicTools.createSpinnerCard(this, "最大帧率", maxFpsAdapter, String.valueOf(AppData.setting.getDefaultMaxFps()), str -> AppData.setting.setDefaultMaxFps(Integer.parseInt(str))).getRoot());
    setActivity.setDefault.addView(PublicTools.createSpinnerCard(this, "最大码率", videoBitAdapter, String.valueOf(AppData.setting.getDefaultVideoBit()), str -> AppData.setting.setDefaultVideoBit(Integer.parseInt(str))).getRoot());
    setActivity.setDefault.addView(PublicTools.createSwitchCard(this, "修改分辨率", AppData.setting.getDefaultSetResolution(), isChecked -> AppData.setting.setDefaultSetResolution(isChecked)).getRoot());
    // 显示
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, "熄屏控制", AppData.setting.getTurnOffScreen(), isChecked -> AppData.setting.setTurnOffScreen(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, "自动屏幕控制", AppData.setting.getAutoControlScreen(), isChecked -> AppData.setting.setAutoControlScreen(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, "默认全屏启动", AppData.setting.getDefaultFull(), isChecked -> AppData.setting.setDefaultFull(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createTextCard(this, "清除默认设备", () -> {
      AppData.setting.setDefaultDevice("");
      Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, "重新生成密钥(需重新授权)", () -> {
      try {
        // 读取密钥文件
        File privateKey = new File(this.getApplicationContext().getFilesDir(), "private.key");
        File publicKey = new File(this.getApplicationContext().getFilesDir(), "public.key");
        AdbKeyPair.generate(privateKey, publicKey);
        AppData.keyPair = AdbKeyPair.read(privateKey, publicKey);
        Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
      } catch (Exception ignored) {
      }
    }).getRoot());
    // 关于
    setActivity.setAbout.addView(PublicTools.createTextCard(this, "查看本机IP", () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, "使用说明", () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, "隐私政策", () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/blob/master/PRIVACY.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, "版本: " + BuildConfig.VERSION_NAME, () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/releases")).getRoot());
  }

  // 浏览器打开
  private void startUrl(String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.addCategory(Intent.CATEGORY_BROWSABLE);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setData(Uri.parse(url));
      startActivity(intent);
    } catch (Exception ignored) {
      Toast.makeText(this, "没有默认浏览器", Toast.LENGTH_SHORT).show();
    }
  }

  // 设置返回按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
    setActivity.setCenter.setOnClickListener(v -> startActivity(new Intent(this, CenterActivity.class)));
  }
}
