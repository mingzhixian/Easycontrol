package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;

import top.saymzx.easycontrol.app.databinding.ActivityActiveBinding;
import top.saymzx.easycontrol.app.databinding.ItemLoadingBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.ActiveHelper;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class ActiveActivity extends Activity {

  private ActivityActiveBinding activityActiveBinding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setLocale(this);
    activityActiveBinding = ActivityActiveBinding.inflate(this.getLayoutInflater());
    setContentView(activityActiveBinding.getRoot());
    // 取消激活
    if (AppData.setting.getIsActive()) deactivate();
    setButtonListener();
    // 绘制UI
    drawUi();
  }

  private void drawUi() {
    activityActiveBinding.key.setText(AppData.setting.getActiveKey());
    activityActiveBinding.url.setOnClickListener(v -> PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/DONATE.md"));
  }

  private void setButtonListener() {
    activityActiveBinding.active.setOnClickListener(v -> {
      String activeKey = String.valueOf(activityActiveBinding.key.getText());
      AppData.setting.setActiveKey(activeKey);
      Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(this);
      loading.second.show();
      new Thread(() -> {
        boolean isOk = ActiveHelper.active(activeKey);
        loading.second.cancel();
        AppData.uiHandler.post(() -> {
          if (isOk) {
            finish();
            AppData.setting.setIsActive(true);
            PublicTools.startUrl(this, "https://gitee.com/mingzhixianweb/easycontrol/blob/master/HOW_TO_USE.md");
            PublicTools.logToast("active", getString(R.string.toast_success), true);
          } else PublicTools.logToast("active", getString(R.string.toast_fail), true);
        });
      }).start();
    });
  }

  // 取消激活
  private void deactivate() {
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(this);
    loading.second.show();
    new Thread(() -> {
      boolean isOk = ActiveHelper.deactivate(AppData.setting.getActiveKey());
      loading.second.cancel();
      AppData.uiHandler.post(() -> {
        if (isOk) {
          AppData.setting.setIsActive(false);
          PublicTools.logToast("deactivate", getString(R.string.toast_success), true);
        } else PublicTools.logToast("deactivate", getString(R.string.toast_fail), true);
      });
    }).start();
  }

  @Override
  public void onBackPressed() {
  }
}