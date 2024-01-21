package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_mini_on_outside), getString(R.string.set_display_default_mini_on_outside_detail), AppData.setting.getDefaultMiniOnOutside(), isChecked -> AppData.setting.setDefaultMiniOnOutside(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_show_nav_bar), getString(R.string.set_display_default_show_nav_bar_detail), AppData.setting.getDefaultShowNavBar(), isChecked -> AppData.setting.setDefaultShowNavBar(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_if_start_default_usb), getString(R.string.set_if_start_default_usb_detail), AppData.setting.getNeedStartDefaultUsbDevice(), isChecked -> AppData.setting.setNeedStartDefaultUsbDevice(isChecked)).getRoot());

    String defaultDevice = AppData.setting.getDefaultDevice();
    if (!defaultDevice.isEmpty()) {
      defaultDevice = AppData.dbHelper.getByUUID(defaultDevice).address;
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), defaultDevice, () -> {
        AppData.setting.setDefaultDevice("");
        setActivity.setOther.removeViewAt(1);
        setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), getString(R.string.set_other_no_default), () -> {
          Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
        }).getRoot(), 1);
        Toast.makeText(this, getString(R.string.set_other_clear_default_code), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }
    else {
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), getString(R.string.set_other_no_default), () -> {
        Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }

    String defaultUsbDevice = AppData.setting.getDefaultUsbDevice();
    if (!defaultUsbDevice.isEmpty()) {
      defaultUsbDevice = AppData.dbHelper.getByUUID(defaultUsbDevice).uuid;
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), defaultUsbDevice, () -> {
        AppData.setting.setDefaultUsbDevice("");
        setActivity.setOther.removeViewAt(2);
        setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), getString(R.string.set_other_no_default), () -> {
          Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
        }).getRoot(), 2);
        Toast.makeText(this, getString(R.string.set_other_clear_default_usb_code), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }
    else {
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), getString(R.string.set_other_no_default), () -> {
        Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }

    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_about_ip), () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
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
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_website), () -> startUrl("https://gitee.com/mingzhixianweb/easycontrol")).getRoot());
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

  // 设置按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
  }
}
