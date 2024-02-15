package top.saymzx.easycontrol.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;

import java.util.ArrayList;

import top.saymzx.easycontrol.app.databinding.ActivityIpBinding;
import top.saymzx.easycontrol.app.databinding.ItemTextBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.helper.PublicTools;
import top.saymzx.easycontrol.app.helper.ViewTools;

public class IpActivity extends Activity {
  private ActivityIpBinding ipActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setLocale(this);
    ipActivity = ActivityIpBinding.inflate(this.getLayoutInflater());
    setContentView(ipActivity.getRoot());
    setButtonListener();
    // 绘制UI
    drawUi();
    super.onCreate(savedInstanceState);
  }

  private void drawUi() {
    // 添加IP
    Pair<ArrayList<String>, ArrayList<String>> listPair = PublicTools.getIp();
    for (String i : listPair.first) {
      ItemTextBinding text = ViewTools.createTextCard(this, i, () -> {
        AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i));
        Toast.makeText(this, getString(R.string.ip_copy), Toast.LENGTH_SHORT).show();
      });
      ipActivity.ipv4.addView(text.getRoot());
    }
    for (String i : listPair.second) {
      ItemTextBinding text = ViewTools.createTextCard(this, i, () -> {
        AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i));
        Toast.makeText(this, getString(R.string.ip_copy), Toast.LENGTH_SHORT).show();
      });
      ipActivity.ipv6.addView(text.getRoot());
    }
  }

  // 设置返回按钮监听
  private void setButtonListener() {
    ipActivity.backButton.setOnClickListener(v -> finish());
  }

}
