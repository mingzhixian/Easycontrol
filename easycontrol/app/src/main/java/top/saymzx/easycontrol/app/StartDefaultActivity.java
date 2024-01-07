package top.saymzx.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

import static top.saymzx.easycontrol.app.client.Client.allClient;

public class StartDefaultActivity extends Activity {
  @SuppressLint("StaticFieldLeak")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 判断是否已经启动
    boolean DefaultStarted = false;
    for (Client client : allClient) {
      if (AppData.setting.getDefaultDevice().equals(client.device.uuid)) {
        DefaultStarted = true;
        break;
      }
    }

    if (!DefaultStarted) {
      // 初始化
      AppData.init(this);
      // 启动默认设备
      if (!AppData.setting.getDefaultDevice().equals("")) {
        Device device = AppData.dbHelper.getByUUID(AppData.setting.getDefaultDevice());
        if (device != null)
          new Client(device);
        else {
          finish();
          AppData.handler.post(() -> Toast.makeText(AppData.main, "默认设备不存在", Toast.LENGTH_SHORT).show());
          return;
        }
      }
      else {
        finish();
        AppData.handler.post(() -> Toast.makeText(AppData.main, "未设置默认设备", Toast.LENGTH_SHORT).show());
        return;
      }

      // 等待启动
      new Thread(() -> {
        while (true) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          // 判断是否已经启动
          for (Client client : allClient) {
            if (AppData.setting.getDefaultDevice().equals(client.device.uuid)) {
              if (client.isStarted()) {
                try {
                  Thread.sleep(500);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                finish();
                return;
              }
            }
          }
        }
      }).start();
    }
    else {
      finish();
    }
  }
}