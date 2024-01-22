package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import top.saymzx.easycontrol.app.databinding.ActivitySetBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    setActivity = ActivitySetBinding.inflate(this.getLayoutInflater());
    setContentView(setActivity.getRoot());
    // 设置页面
    drawUi();
    setButtonListener();
    super.onCreate(savedInstanceState);
  }

  // 设置默认值
  private void drawUi() {
    // 默认参数
    PublicTools.createDeviceOptionSet(this, setActivity.setDefault, null);
    // 显示
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_keep_screen_awake), getString(R.string.set_display_keep_screen_awake_detail), AppData.setting.getKeepAwake(), isChecked -> AppData.setting.setKeepAwake(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_auto_back_on_start_default), getString(R.string.set_display_auto_back_on_start_default_detail), AppData.setting.getAutoBackOnStartDefault(), isChecked -> AppData.setting.setAutoBackOnStartDefault(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_mini_on_outside), getString(R.string.set_display_default_mini_on_outside_detail), AppData.setting.getAutoMiniOnOutside(), isChecked -> AppData.setting.setAutoMiniOnOutside(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_show_nav_bar), getString(R.string.set_display_default_show_nav_bar_detail), AppData.setting.getDefaultShowNavBar(), isChecked -> AppData.setting.setDefaultShowNavBar(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_about_ip), () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_clear_default), () -> {
      AppData.setting.setDefaultDevice("");
      Toast.makeText(this, getString(R.string.set_other_clear_default_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_custom_key), () -> startActivity(new Intent(this, AdbKeyActivity.class))).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_clear_key), () -> {
      AppData.reGenerateAdbKeyPair(this);
      Toast.makeText(this, getString(R.string.set_other_clear_key_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_locale), () -> {
      AppData.setting.setDefaultLocale(!AppData.setting.getDefaultLocale().equals("zh") ? "zh" : "en");
      Toast.makeText(this, getString(R.string.set_other_locale_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    // 关于
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_active), () -> startActivity(new Intent(this, ActiveActivity.class))).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_website), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_how_to_use), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_privacy), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/PRIVACY.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_version) + BuildConfig.VERSION_NAME, () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/releases")).getRoot());
  }

  // 设置按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
  }
}
