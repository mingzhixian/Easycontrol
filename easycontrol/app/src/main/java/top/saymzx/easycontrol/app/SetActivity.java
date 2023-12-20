package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;

import top.saymzx.easycontrol.adb.AdbKeyPair;
import top.saymzx.easycontrol.app.client.Client;
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
    // 默认参数
    PublicTools.createDeviceOptionSet(this, setActivity.setDefault, null);
    // 显示
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_show_nav_bar), getString(R.string.set_display_default_show_nav_bar_detail), AppData.setting.getDefaultShowNavBar(), isChecked -> AppData.setting.setDefaultShowNavBar(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_master_audo_rotation), getString(R.string.set_display_master_audo_rotation_detail), AppData.setting.getMasterAudoRotation(), isChecked -> AppData.setting.setMasterAudoRotation(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_slave_audo_rotation), getString(R.string.set_display_slave_audo_rotation_detail), AppData.setting.getSlaveAudoRotation(), isChecked -> AppData.setting.setSlaveAudoRotation(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_clear_default), () -> {
      AppData.setting.setDefaultDevice("");
      Toast.makeText(this, getString(R.string.set_other_clear_default_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_clear_key), () -> {
      try {
        // 读取密钥文件
        File privateKey = new File(this.getApplicationContext().getFilesDir(), "private.key");
        File publicKey = new File(this.getApplicationContext().getFilesDir(), "public.key");
        AdbKeyPair.generate(privateKey, publicKey);
        AppData.keyPair = AdbKeyPair.read(privateKey, publicKey);
        Toast.makeText(this, getString(R.string.set_other_clear_key_code), Toast.LENGTH_SHORT).show();
      } catch (Exception ignored) {
      }
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_local_recover), () -> {
      int port = AppData.setting.getCenterAdbPort();
      if (port == -1) Toast.makeText(this, getString(R.string.set_other_local_recover_code_error_adb), Toast.LENGTH_SHORT).show();
      else Client.recover("127.0.0.1:" + port, result -> Toast.makeText(this, getString(result ? R.string.set_other_local_recover_code_success : R.string.set_other_local_recover_code_error_connect), Toast.LENGTH_SHORT).show());
    }).getRoot());
    // 关于
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_ip), () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_how_to_use), () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_privacy), () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/blob/master/PRIVACY.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_version) + BuildConfig.VERSION_NAME, () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol/releases")).getRoot());
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
      Toast.makeText(this, getString(R.string.error_no_browser), Toast.LENGTH_SHORT).show();
    }
  }

  // 设置返回按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
    setActivity.setCenter.setOnClickListener(v -> startActivity(new Intent(this, CenterActivity.class)));
  }
}
