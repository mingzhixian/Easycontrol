package top.saymzx.easycontrol.app.helper;

import android.app.Dialog;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Objects;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  // 特殊设备的名单，在该名单的设备则显示，不在则不显示
  public ArrayList<String> centerDevices = new ArrayList<>();
  public Pair<String, UsbDevice> linkDevice = null;

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
    if (device.isLinkDevice()) setView(view, device, linkDevice.second, R.drawable.link);
    else if (device.isCenterDevice()) setView(view, device, null, R.drawable.center);
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
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = PublicTools.createDialog(context, true, itemSetDeviceBinding.getRoot());
    itemSetDeviceBinding.open.setOnClickListener(v -> {
      dialog.cancel();
      new Client(device, null);
    });
    itemSetDeviceBinding.defult.setOnClickListener(v -> {
      dialog.cancel();
      if (!device.isNormalDevice()) return;
      AppData.setting.setDefaultDevice(device.uuid);
    });
    itemSetDeviceBinding.delete.setOnClickListener(v -> {
      AppData.dbHelper.delete(device);
      update();
      dialog.cancel();
    });
    itemSetDeviceBinding.change.setOnClickListener(v -> {
      dialog.cancel();
      PublicTools.createAddDeviceView(context, device, this).show();
    });
    dialog.show();
  }

  private void queryDevices() {
    ArrayList<Device> rawDevices = AppData.dbHelper.getAll();
    Device tmp1 = null;
    ArrayList<Device> tmp2 = new ArrayList<>();
    ArrayList<Device> tmp3 = new ArrayList<>();
    for (Device device : rawDevices) {
      if (device.isLinkDevice()) {
        if (linkDevice != null && Objects.equals(device.uuid, linkDevice.first)) tmp1 = device;
      } else if (device.isCenterDevice()) {
        if (centerDevices.contains(device.uuid)) tmp2.add(device);
      } else {
        tmp3.add(device);
      }
    }
    devices.clear();
    if (tmp1 != null) devices.add(tmp1);
    devices.addAll(tmp2);
    devices.addAll(tmp3);
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
