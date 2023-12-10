package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    // 默认参数
    PublicTools.createDeviceOptionSet(this, setActivity.setDefault, null);
    // 显示
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, "主控端自动旋转", AppData.setting.getMasterAudoRotation(), isChecked -> AppData.setting.setMasterAudoRotation(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, "被控端方向跟随", AppData.setting.getSlaveAudoRotation(), isChecked -> AppData.setting.setSlaveAudoRotation(isChecked)).getRoot());
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
