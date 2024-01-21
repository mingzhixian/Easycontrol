package top.saymzx.easycontrol.app.helper;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  private final ArrayList<Device> devices = new ArrayList<>();
  public ArrayList<Pair<String, UsbDevice>> linkDevices = new ArrayList<>();
  private final Context context;

  public DeviceListAdapter(Context c) {
    queryDevices();
    context = c;
  }

  @Override
  public int getCount() {
    return devices.size();
  }

  @Override
  public Object getItem(int i) {
    return null;
  }

  @Override
  public long getItemId(int i) {
    return 0;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    if (view == null) {
      ItemDevicesItemBinding devicesItemBinding = ItemDevicesItemBinding.inflate(LayoutInflater.from(context));
      view = devicesItemBinding.getRoot();
      view.setTag(devicesItemBinding);
    }
    // 获取设备
    Device device = devices.get(i);
    if (device.isLinkDevice()) {
      for (Pair<String, UsbDevice> pair : linkDevices) {
        if (Objects.equals(pair.first, device.uuid)) {
          setView(view, device, pair.second, R.drawable.link);
          return view;
        }
      }
    }
    else setView(view, device, null, R.drawable.phone);
    return view;
  }

  // 创建View
  private void setView(View view, Device device, UsbDevice usbDevice, int deviceIcon) {
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    devicesItemBinding.deviceIcon.setImageResource(deviceIcon);
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> new Client(device, usbDevice));
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
  }

  // 卡片长按事件
  private void onLongClickCard(Device device) {
    Pair<String, UsbDevice> linkDevice = null;
    Pair<String, UsbDevice> linkDevice_final;
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = PublicTools.createDialog(context, true, itemSetDeviceBinding.getRoot());

    for (Pair<String, UsbDevice> pair : linkDevices) {
      if (Objects.equals(pair.first, device.uuid)) {
        linkDevice = pair;
        break;
      }
    }
    linkDevice_final = linkDevice;

    // 有线设备
    if (device.isLinkDevice()) {
      itemSetDeviceBinding.buttonStartWireless.setVisibility(View.VISIBLE);
      itemSetDeviceBinding.buttonSetDefault.setText(R.string.set_device_button_set_default_link);
      itemSetDeviceBinding.buttonStartWireless.setOnClickListener(v -> {
        dialog.cancel();
        Client.restartOnTcpip(device, linkDevice_final.second, result -> AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_device_button_start_wireless_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
      });

      itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
        dialog.cancel();
        if (!device.isLinkDevice()) return;
        AppData.setting.setDefaultUsbDevice(device.uuid);
      });
    } else {
      itemSetDeviceBinding.buttonStartWireless.setVisibility(View.GONE);
      itemSetDeviceBinding.buttonSetDefault.setText(R.string.set_device_button_set_default);

      itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
        dialog.cancel();
        if (!device.isNormalDevice()) return;
        AppData.setting.setDefaultDevice(device.uuid);
      });
    }
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      Client.runOnceCmd(device, device.isLinkDevice() ? linkDevice_final.second : null, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_device_button_recover_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
    });
    itemSetDeviceBinding.buttonGetUuid.setOnClickListener(v -> {
      dialog.cancel();
      AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, device.uuid));
      Toast.makeText(AppData.main, AppData.main.getString(R.string.set_device_button_get_uuid_success), Toast.LENGTH_SHORT).show();
    });
    itemSetDeviceBinding.buttonChange.setOnClickListener(v -> {
      dialog.cancel();
      PublicTools.createAddDeviceView(context, device, this).show();
    });
    itemSetDeviceBinding.buttonDelete.setOnClickListener(v -> {
      AppData.dbHelper.delete(device);
      update();
      dialog.cancel();
    });
    dialog.show();
  }

  private void queryDevices() {
    ArrayList<Device> rawDevices = AppData.dbHelper.getAll();
    ArrayList<Device> tmp1 = new ArrayList<>();
    ArrayList<Device> tmp2 = new ArrayList<>();
    for (Device device : rawDevices) {
      if (device.isLinkDevice() && linkDevices != null) {
        for (Pair<String, UsbDevice> pair : linkDevices) {
          if (Objects.equals(pair.first, device.uuid)) {
            tmp1.add(device);
            break;
          }
        }
      }
      else if (device.isNormalDevice()) tmp2.add(device);
    }
    devices.clear();
    devices.addAll(tmp1);
    devices.addAll(tmp2);
  }

  public void startByUUID(String uuid) {
    for (Device device : devices) {
      if (Objects.equals(device.uuid, uuid)) {
        if (device.isLinkDevice()) {
          for (Pair<String, UsbDevice> pair : linkDevices) {
            if (Objects.equals(pair.first, device.uuid)) {
              new Client(device, pair.second);
              return;
            }
          }
        }
        else new Client(device, null);
      }
    }
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
