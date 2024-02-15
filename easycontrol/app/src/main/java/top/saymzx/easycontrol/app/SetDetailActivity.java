package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Objects;

import top.saymzx.easycontrol.app.databinding.ActivitySetDetailBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class SetDetailActivity extends Activity {
  private ActivitySetDetailBinding setDetailActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setLocale(this);
    setDetailActivity = ActivitySetDetailBinding.inflate(this.getLayoutInflater());
    setContentView(setDetailActivity.getRoot());
    setButtonListener();
    // 检查类型
    String type = getIntent().getStringExtra("type");
    if (Objects.equals(type, "default")) drawDefault();
    else if (Objects.equals(type, "onStart")) drawOnStart();
    else if (Objects.equals(type, "onConnect")) drawOnConnect();
    else if (Objects.equals(type, "onClose")) drawOnClose();
    else if (Objects.equals(type, "connecting")) drawConnecting();
    else if (Objects.equals(type, "adbKey")) drawAdbKey();
    else if (Objects.equals(type, "about")) drawAbout();
    super.onCreate(savedInstanceState);
  }

  // 设置按钮监听
  private void setButtonListener() {
    setDetailActivity.backButton.setOnClickListener(v -> finish());
  }

  // 绘制默认参数
  private void drawDefault() {
    ViewTools.createDeviceOptionSet(this, setDetailActivity.setDetail, null);
  }

  // 绘制启动时操作
  private void drawOnStart() {
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_back_after_start_default_on_start), getString(R.string.set_auto_back_after_start_default_on_start_detail), AppData.setting.getAutoBackOnStart(), isChecked -> AppData.setting.setAutoBackOnStart(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_auto_clear_default), () -> {
      AppData.setting.setDefaultDevice("");
      Toast.makeText(this, getString(R.string.set_auto_clear_default_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_scan_address_on_start), getString(R.string.set_auto_scan_address_on_start_detail), AppData.setting.getAutoScanAddressOnStart(), isChecked -> AppData.setting.setAutoScanAddressOnStart(isChecked)).getRoot());
  }

  // 绘制连接时操作
  private void drawOnConnect() {
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_wake_on_connect), getString(R.string.set_auto_wake_on_connect_detail), AppData.setting.getWakeOnConnect(), isChecked -> AppData.setting.setWakeOnConnect(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_lightoff_on_connect), getString(R.string.set_auto_lightoff_on_connect_detail), AppData.setting.getLightOffOnConnect(), isChecked -> AppData.setting.setLightOffOnConnect(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_show_nav_bar_on_connect), getString(R.string.set_auto_show_nav_bar_on_connect_detail), AppData.setting.getShowNavBarOnConnect(), isChecked -> AppData.setting.setShowNavBarOnConnect(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_change_to_full_on_connect), getString(R.string.set_auto_change_to_full_on_connect_detail), AppData.setting.getChangeToFullOnConnect(), isChecked -> AppData.setting.setChangeToFullOnConnect(isChecked)).getRoot());
  }

  // 绘制断开时操作
  private void drawOnClose() {
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_lock_on_close), getString(R.string.set_auto_lock_on_close_detail), AppData.setting.getLockOnClose(), isChecked -> AppData.setting.setLockOnClose(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_light_on_close), getString(R.string.set_auto_light_on_close_detail), AppData.setting.getLightOnClose(), isChecked -> AppData.setting.setLightOnClose(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_reconnect_on_close), getString(R.string.set_auto_reconnect_on_close_detail), AppData.setting.getReconnectOnClose(), isChecked -> AppData.setting.setReconnectOnClose(isChecked)).getRoot());
  }

  // 绘制界面显示
  private void drawConnecting() {
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_small_to_mini_on_outside), getString(R.string.set_auto_small_to_mini_on_outside_detail), AppData.setting.getSmallToMiniOnOutside(), isChecked -> AppData.setting.setSmallToMiniOnOutside(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_full_to_mini_on_exit), getString(R.string.set_auto_full_to_mini_on_exit_detail), AppData.setting.getFullToMiniOnExit(), isChecked -> AppData.setting.setFullToMiniOnExit(isChecked)).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createSwitchCard(this, getString(R.string.set_auto_mini_recover_on_timeout), getString(R.string.set_auto_mini_recover_on_timeout_detail), AppData.setting.getMiniRecoverOnTimeout(), isChecked -> AppData.setting.setMiniRecoverOnTimeout(isChecked)).getRoot());
  }

  // 绘制密钥相关操作
  private void drawAdbKey() {
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_other_custom_key), () -> startActivity(new Intent(this, AdbKeyActivity.class))).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_other_clear_key), () -> {
      AppData.keyPair = PublicTools.reGenerateAdbKeyPair();
      Toast.makeText(this, getString(R.string.set_other_clear_key_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
  }

  // 绘制关于
  private void drawAbout() {
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_about_active), () -> startActivity(new Intent(this, ActiveActivity.class))).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_about_website), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol")).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_about_how_to_use), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md")).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_about_privacy), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/PRIVACY.md")).getRoot());
    setDetailActivity.setDetail.addView(ViewTools.createTextCard(this, getString(R.string.set_about_version) + BuildConfig.VERSION_NAME, () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/releases")).getRoot());
  }
}