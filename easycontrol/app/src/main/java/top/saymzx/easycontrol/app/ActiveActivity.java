package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;

import top.saymzx.easycontrol.app.databinding.ActivityActiveBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.ActiveHelper;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class ActiveActivity extends Activity {

  private ActivityActiveBinding activeActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setLocale(this);
    activeActivity = ActivityActiveBinding.inflate(this.getLayoutInflater());
    setContentView(activeActivity.getRoot());
    // 取消激活
    if (AppData.setting.getIsActive()) deactivate();
    setButtonListener();
    // 绘制UI
    drawUi();
    super.onCreate(savedInstanceState);
  }

  private void drawUi() {
    activeActivity.key.setText(AppData.setting.getActiveKey());
    activeActivity.layout.addView(ViewTools.createTextCard(this, getString(R.string.active_url), () -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/DONATE.md")).getRoot());
  }

  private void setButtonListener() {
    activeActivity.active.setOnClickListener(v -> {
      String activeKey = String.valueOf(activeActivity.key.getText());
      AppData.setting.setActiveKey(activeKey);
      Pair<View, WindowManager.LayoutParams> loading = ViewTools.createLoading(this);
      AppData.windowManager.addView(loading.first, loading.second);
      new Thread(() -> {
        boolean isOk = ActiveHelper.active(activeKey);
        AppData.windowManager.removeView(loading.first);
        AppData.uiHandler.post(() -> {
          if (isOk) {
            finish();
            AppData.setting.setIsActive(true);
            PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md");
            PublicTools.logToast("active", getString(R.string.active_button_success), true);
          } else PublicTools.logToast("active", getString(R.string.active_button_error), true);
        });
      }).start();
    });
  }

  // 取消激活
  private void deactivate() {
    Pair<View, WindowManager.LayoutParams> loading = ViewTools.createLoading(this);
    AppData.windowManager.addView(loading.first, loading.second);
    new Thread(() -> {
      boolean isOk = ActiveHelper.deactivate(AppData.setting.getActiveKey());
      AppData.windowManager.removeView(loading.first);
      AppData.uiHandler.post(() -> {
        if (isOk) {
          AppData.setting.setIsActive(false);
          PublicTools.logToast("deactivate", getString(R.string.active_deactivate_success), true);
        } else PublicTools.logToast("deactivate", getString(R.string.active_deactivate_error), true);
      });
    }).start();
  }

  @Override
  public void onBackPressed() {
  }
}