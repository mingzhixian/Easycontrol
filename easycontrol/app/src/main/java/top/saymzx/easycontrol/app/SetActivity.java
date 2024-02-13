package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import top.saymzx.easycontrol.app.databinding.ActivitySetBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setLocale(this);
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
    setActivity.setDefault.addView(ViewTools.createTextCard(this, getString(R.string.set_default), () -> enterDetailSet("default")).getRoot());
    // 显示
    setActivity.setAuto.addView(ViewTools.createTextCard(this, getString(R.string.set_on_start), () -> enterDetailSet("onStart")).getRoot());
    setActivity.setAuto.addView(ViewTools.createTextCard(this, getString(R.string.set_on_connect), () -> enterDetailSet("onConnect")).getRoot());
    setActivity.setAuto.addView(ViewTools.createTextCard(this, getString(R.string.set_on_close), () -> enterDetailSet("onClose")).getRoot());
    setActivity.setAuto.addView(ViewTools.createTextCard(this, getString(R.string.set_connecting), () -> enterDetailSet("connecting")).getRoot());
    // 其他
    setActivity.setOther.addView(ViewTools.createTextCard(this, getString(R.string.set_about_ip), () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
    setActivity.setOther.addView(ViewTools.createTextCard(this, getString(R.string.set_adb_key), () -> enterDetailSet("adbKey")).getRoot());
    // 关于
    setActivity.setAbout.addView(ViewTools.createTextCard(this, getString(R.string.set_other_locale), () -> {
      AppData.setting.setDefaultLocale(AppData.setting.getDefaultLocale().equals("en") ? "zh" : "en");
      Toast.makeText(this, getString(R.string.set_other_locale_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setAbout.addView(ViewTools.createTextCard(this, getString(R.string.set_about), () -> enterDetailSet("about")).getRoot());
  }

  // 设置按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
  }

  // 进入详细设置页面
  private void enterDetailSet(String type) {
    Intent tmpIntent = new Intent(this, SetDetailActivity.class);
    tmpIntent.putExtra("type", type);
    startActivity(tmpIntent);
  }
}
