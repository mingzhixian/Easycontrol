package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import top.saymzx.easycontrol.app.databinding.ActivityActiveBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.ActiveHelper;
import top.saymzx.easycontrol.app.helper.PublicTools;

public class ActiveActivity extends Activity {

  private ActivityActiveBinding activeActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    activeActivity = ActivityActiveBinding.inflate(this.getLayoutInflater());
    setContentView(activeActivity.getRoot());
    AppData.setting.setIsActive(false);
    setButtonListener();
    // 绘制UI
    drawUi();
    super.onCreate(savedInstanceState);
  }

  private void drawUi() {
    activeActivity.key.setText(AppData.setting.getActiveKey());
    activeActivity.layout.addView(PublicTools.createTextCard(this, getString(R.string.active_url), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/DONATE.md")).getRoot());
  }

  private void setButtonListener() {
    activeActivity.active.setOnClickListener(v -> {
      String activeKey = String.valueOf(activeActivity.key.getText());
      AppData.setting.setActiveKey(activeKey);
      Dialog dialog = PublicTools.createClientLoading(AppData.main);
      dialog.show();
      new Thread(() -> {
        boolean isOk = ActiveHelper.checkOk(activeKey);
        dialog.cancel();
        AppData.uiHandler.post(() -> {
          if (isOk) {
            finish();
            AppData.setting.setIsActive(true);
            Toast.makeText(this, getString(R.string.active_button_success), Toast.LENGTH_SHORT).show();
            PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md");
          } else Toast.makeText(this, getString(R.string.active_button_error), Toast.LENGTH_SHORT).show();
        });
      }).start();
    });
  }

  @Override
  public void onBackPressed() {
  }
}