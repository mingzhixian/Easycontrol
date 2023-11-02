package top.saymzx.easycontrol.app.helper;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  ArrayList<Device> devices;
  Context context;

  public DeviceListAdapter(Context c, ArrayList<Device> d) {
    devices = d;
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
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    Device device = devices.get(i);
    // 设置卡片值
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> new Client(device, null));
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
    return view;
  }

  // 卡片长按事件
  private void onLongClickCard(Device device) {
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = AppData.publicTools.createDialog(context,true, itemSetDeviceBinding.getRoot());
    itemSetDeviceBinding.open.setOnClickListener(v -> {
      dialog.hide();
      new Client(device, null);
    });
    itemSetDeviceBinding.defult.setOnClickListener(v -> {
      dialog.hide();
      AppData.setting.setDefaultDevice(device.id);
    });
    itemSetDeviceBinding.delete.setOnClickListener(v -> {
      AppData.dbHelper.delete(device);
      update();
      dialog.hide();
    });
    itemSetDeviceBinding.change.setOnClickListener(v -> {
      dialog.hide();
      Dialog addDeviceDialog = AppData.publicTools.createAddDeviceView(context, device, this);
      addDeviceDialog.show();
    });
    dialog.show();
  }

  public void update() {
    devices = AppData.dbHelper.getAll();
    notifyDataSetChanged();
  }

}
