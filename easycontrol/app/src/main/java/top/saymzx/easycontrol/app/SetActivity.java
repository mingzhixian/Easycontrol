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

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setActivity = ActivitySetBinding.inflate(this.getLayoutInflater());
    setContentView(setActivity.getRoot());
    // 设置状态栏导航栏颜色沉浸
    AppData.publicTools.setStatusAndNavBar(this);
    // 设置页面
    drawUi();
    setButtonListener();
  }

  // 设置默认值
  private void drawUi() {
    ArrayAdapter<String> maxSizeAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, AppData.publicTools.maxSizeList);
    ArrayAdapter<String> maxFpsAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, AppData.publicTools.maxFpsList);
    ArrayAdapter<String> videoBitAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, AppData.publicTools.videoBitList);
    // 默认配置
    setActivity.setDefault.addView(
      AppData.publicTools.createSpinnerCard(
        this,
        "最大大小",
        maxSizeAdapter,
        String.valueOf(AppData.setting.getDefaultMaxSize()),
        str -> AppData.setting.setDefaultMaxSize(Integer.parseInt(str))
      ).getRoot()
    );
    setActivity.setDefault.addView(
      AppData.publicTools.createSpinnerCard(
        this,
        "最大帧率",
        maxFpsAdapter,
        String.valueOf(AppData.setting.getDefaultMaxFps()),
        str -> AppData.setting.setDefaultMaxFps(Integer.parseInt(str))
      ).getRoot()
    );
    setActivity.setDefault.addView(
      AppData.publicTools.createSpinnerCard(
        this,
        "最大码率",
        videoBitAdapter,
        String.valueOf(AppData.setting.getDefaultVideoBit()),
        str -> AppData.setting.setDefaultVideoBit(Integer.parseInt(str))
      ).getRoot()
    );
    setActivity.setDefault.addView(
      AppData.publicTools.createSwitchCard(
        this,
        "修改分辨率",
        AppData.setting.getDefaultSetResolution(),
        isChecked -> AppData.setting.setDefaultSetResolution(isChecked)
      ).getRoot()
    );
    // 显示
    setActivity.setDisplay.addView(
      AppData.publicTools.createSwitchCard(
        this,
        "被控端熄屏",
        AppData.setting.getSlaveTurnOffScreen(),
        isChecked -> AppData.setting.setSlaveTurnOffScreen(isChecked)
      ).getRoot()
    );
    setActivity.setDisplay.addView(
      AppData.publicTools.createSwitchCard(
        this,
        "默认全屏启动",
        AppData.setting.getDefaultFull(),
        isChecked -> AppData.setting.setDefaultFull(isChecked)
      ).getRoot()
    );
    // 其他
    setActivity.setOther.addView(
      AppData.publicTools.createTextCard(
        this,
        "清除默认设备",
        () -> {
          AppData.setting.setDefaultDevice(-1);
          Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show();
        }
      ).getRoot()
    );
    setActivity.setOther.addView(
      AppData.publicTools.createTextCard(
        this,
        "重新生成密钥(需重新授权)",
        () -> {
          try {
            // 读取密钥文件
            File privateKey = new File(this.getApplicationContext().getFilesDir(), "private.key");
            File publicKey = new File(this.getApplicationContext().getFilesDir(), "public.key");
            AdbKeyPair.generate(privateKey, publicKey);
            AppData.keyPair = AdbKeyPair.read(privateKey, publicKey);
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
          } catch (Exception ignored) {
          }

        }
      ).getRoot()
    );
    // 关于
    setActivity.setAbout.addView(
      AppData.publicTools.createTextCard(
        this,
        "查看本机IP",
        () -> startActivity(new Intent(this, IpActivity.class))
      ).getRoot()
    );
    setActivity.setAbout.addView(
      AppData.publicTools.createTextCard(
        this,
        "使用说明",
        () -> startUrl("https://github.com/mingzhixian/Easycontrol/blob/master/HOW_TO_USE.md")
      ).getRoot()
    );
    setActivity.setAbout.addView(
      AppData.publicTools.createTextCard(
        this,
        "隐私政策",
        () -> startUrl("https://github.com/mingzhixian/Easycontrol/blob/master/PRIVACY.md")
      ).getRoot()
    );
    setActivity.setAbout.addView(
      AppData.publicTools.createTextCard(
        this,
        "版本: " + BuildConfig.VERSION_NAME,
        () -> startUrl("https://github.com/mingzhixian/Easycontrol/releases")
      ).getRoot()
    );
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
  }
}
