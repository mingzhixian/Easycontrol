package top.saymzx.easycontrol.app.helper;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.util.ArrayList;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  // 特殊设备的名单，在该名单的设备则显示，不在则不显示
  public ArrayList<String> centerDevices = new ArrayList<>();

  private final ArrayList<Device> devices = new ArrayList<>();
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
    if (device.isCenterDevice()) setView(view, device, R.drawable.cloud);
    else setView(view, device, R.drawable.phone);
    return view;
  }

  // 创建View
  private void setView(View view, Device device, int deviceIcon) {
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    devicesItemBinding.deviceIcon.setImageResource(deviceIcon);
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> new Client(device));
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
  }

  // 卡片长按事件
  private void onLongClickCard(Device device) {
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = PublicTools.createDialog(context, true, itemSetDeviceBinding.getRoot());
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      Client.recover(device.address, result -> AppData.handler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_other_local_recover_code_success : R.string.set_other_local_recover_code_error_connect), Toast.LENGTH_SHORT).show()));
    });
    itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
      dialog.cancel();
      if (!device.isNormalDevice()) return;
      AppData.setting.setDefaultDevice(device.uuid);
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
      if (device.isCenterDevice() && centerDevices.contains(device.uuid)) tmp1.add(device);
      else if (device.isNormalDevice()) tmp2.add(device);
    }
    devices.clear();
    devices.addAll(tmp1);
    devices.addAll(tmp2);
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
