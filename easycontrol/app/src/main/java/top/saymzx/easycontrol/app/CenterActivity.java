package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

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
      loginActivity.centerPassword.setText(AppData.setting.getCenterPassword());
      int adbPort = AppData.setting.getCenterAdbPort();
      if (adbPort != -1) loginActivity.centerAdbPort.setText(String.valueOf(adbPort));
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    loginActivity.ok.setOnClickListener(v -> {
      String centerAddress = String.valueOf(loginActivity.centerAddress.getText());
      String centerName = String.valueOf(loginActivity.centerName.getText());
      String centerPassword = String.valueOf(loginActivity.centerPassword.getText());
      String centerAdbPort = String.valueOf(loginActivity.centerAdbPort.getText());
      if (centerAddress.equals("") && centerName.equals("") && centerPassword.equals("")) return;
      AppData.setting.setCenterAddress(centerAddress);
      AppData.setting.setCenterName(centerName);
      AppData.setting.setCenterPassword(centerPassword);
      if (!centerAdbPort.equals(""))
        AppData.setting.setCenterAdbPort(Integer.parseInt(centerAdbPort));
      Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
      CenterHelper.checkCenter();
    });
  }
}