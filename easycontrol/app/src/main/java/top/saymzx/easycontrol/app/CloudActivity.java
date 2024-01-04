package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import top.saymzx.easycontrol.app.databinding.ActivityCloudBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class CloudActivity extends Activity {

  // 创建界面
  private ActivityCloudBinding cloudActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    cloudActivity = ActivityCloudBinding.inflate(this.getLayoutInflater());
    setContentView(cloudActivity.getRoot());
    // 设置状态栏导航栏颜色沉浸
    PublicTools.setStatusAndNavBar(this);
    // 绘制UI
    drawUI();
    // 设置监听
    setButtonListener();
  }

  // 设置值
  private void drawUI() {
    cloudActivity.cloudAddress.setText(AppData.setting.getCloudAddress());
    cloudActivity.cloudName.setText(AppData.setting.getCloudName());
    int adbPort = AppData.setting.getLocalAdbPort();
    if (adbPort != -1) cloudActivity.localAdbPort.setText(String.valueOf(adbPort));
  }

  // 设置按钮监听
  private void setButtonListener() {
    cloudActivity.backButton.setOnClickListener(v -> finish());
    cloudActivity.ok.setOnClickListener(v -> {
      String cloudAddress = String.valueOf(cloudActivity.cloudAddress.getText());
      String cloudName = String.valueOf(cloudActivity.cloudName.getText());
      String cloudPassword = String.valueOf(cloudActivity.cloudPassword.getText());
      String localAdbPort = String.valueOf(cloudActivity.localAdbPort.getText());
      AppData.setting.setCloudAddress(cloudAddress);
      AppData.setting.setCloudName(cloudName);
      if (!cloudPassword.equals("")) AppData.setting.setCloudPassword(md5Encode(cloudPassword));
      AppData.setting.setLocalAdbPort(localAdbPort.equals("") ? -1 : Integer.parseInt(localAdbPort));
      Toast.makeText(this, getString(R.string.center_button_code), Toast.LENGTH_SHORT).show();
    });
  }

  // MD5加密
  private static final String SALT = "Easycontrol";

  private String md5Encode(String str) {
    try {
      return new BigInteger(1, MessageDigest.getInstance("md5").digest((str += SALT).getBytes(StandardCharsets.UTF_8))).toString(16);
    } catch (Exception ignored) {
      return str;
    }
  }
}