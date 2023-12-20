package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import top.saymzx.easycontrol.app.databinding.ActivityCenterBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.CenterHelper;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class CenterActivity extends Activity {

  // 创建界面
  private ActivityCenterBinding loginActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    loginActivity = ActivityCenterBinding.inflate(this.getLayoutInflater());
    setContentView(loginActivity.getRoot());
    // 设置状态栏导航栏颜色沉浸
    PublicTools.setStatusAndNavBar(this);
    // 绘制UI
    drawUI();
    // 设置监听
    setButtonListener();
  }

  // 设置值
  private void drawUI() {
    String centerAddress = AppData.setting.getCenterAddress();
    if (!centerAddress.equals("")) {
      loginActivity.centerAddress.setText(centerAddress);
      loginActivity.centerName.setText(AppData.setting.getCenterName());
      int adbPort = AppData.setting.getCenterAdbPort();
      if (adbPort != -1) loginActivity.centerAdbPort.setText(String.valueOf(adbPort));
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    loginActivity.backButton.setOnClickListener(v -> finish());
    loginActivity.ok.setOnClickListener(v -> {
      String centerAddress = String.valueOf(loginActivity.centerAddress.getText());
      String centerName = String.valueOf(loginActivity.centerName.getText());
      String centerPassword = String.valueOf(loginActivity.centerPassword.getText());
      String centerAdbPort = String.valueOf(loginActivity.centerAdbPort.getText());
      if (centerAddress.equals("") && centerName.equals("") && centerPassword.equals("")) return;
      AppData.setting.setCenterAddress(centerAddress);
      AppData.setting.setCenterName(centerName);
      if (!centerPassword.equals("")) AppData.setting.setCenterPassword(md5Encode(centerPassword));
      AppData.setting.setCenterAdbPort(centerAdbPort.equals("") ? -1 : Integer.parseInt(centerAdbPort));
      Dialog dialog = PublicTools.createClientLoading(this);
      dialog.show();
      CenterHelper.checkCenter(str -> runOnUiThread(() -> {
        dialog.cancel();
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
      }));
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