package top.saymzx.easycontrol.app.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import top.saymzx.easycontrol.app.DeviceDetailActivity;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.ClientStream;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  public final ArrayList<Device> devicesList = new ArrayList<>();
  public final HashMap<String, UsbDevice> linkDevices = new HashMap<>();
  private final Context context;
  private static final int[] colors = {R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color5, R.color.color6, R.color.color7, R.color.color8};

  // 生成随机数
  private final Random random = new Random();

  public DeviceListAdapter(Context c) {
    queryDevices();
    context = c;
  }

  @Override
  public int getCount() {
    return devicesList.size();
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
    Device device = devicesList.get(i);
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    devicesItemBinding.deviceIconBackground.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(colors[random.nextInt(8)])));
    devicesItemBinding.deviceIcon.setImageResource(device.isNetworkDevice() ? R.drawable.wifi : R.drawable.link);
    devicesItemBinding.deviceType.setText(context.getString(device.isNetworkDevice() ? R.string.main_device_type_network : R.string.main_device_type_link));
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> startDevice(device));
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
    Dialog dialog = ViewTools.createDialog(context, true, itemSetDeviceBinding.getRoot());
    // 有线设备
    if (device.isLinkDevice()) {
      itemSetDeviceBinding.buttonStartWireless.setVisibility(View.VISIBLE);
      itemSetDeviceBinding.buttonStartWireless.setOnClickListener(v -> {
        dialog.cancel();
        UsbDevice usbDevice = linkDevices.get(device.uuid);
        if (usbDevice == null) return;
        ClientStream.restartOnTcpip(device, usbDevice, result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show()));
      });
    } else itemSetDeviceBinding.buttonStartWireless.setVisibility(View.GONE);
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      if (device.isLinkDevice()) {
        UsbDevice usbDevice = linkDevices.get(device.uuid);
        if (usbDevice == null) return;
        ClientStream.runOnceCmd(device, usbDevice, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show()));
      } else ClientStream.runOnceCmd(device, null, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show()));
    });
    itemSetDeviceBinding.buttonChange.setOnClickListener(v -> {
      dialog.cancel();
      Intent intent = new Intent(context, DeviceDetailActivity.class);
      intent.putExtra("uuid", device.uuid);
      context.startActivity(intent);
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
      if (device.isLinkDevice() && linkDevices.containsKey(device.uuid)) tmp1.add(device);
      else if (device.isNetworkDevice()) tmp2.add(device);
    }
    devicesList.clear();
    devicesList.addAll(tmp1);
    devicesList.addAll(tmp2);
  }

  public void startByUUID(String uuid) {
    for (Device device : devicesList) if (Objects.equals(device.uuid, uuid)) startDevice(device);
  }

  public void startDevice(Device device) {
    if (device.isLinkDevice()) {
      UsbDevice usbDevice = linkDevices.get(device.uuid);
      if (usbDevice == null) return;
      new Client(device, usbDevice);
    } else new Client(device);
  }

  public void startDefaultDevice() {
    for (Device device : devicesList) if (device.connectOnStart) startDevice(device);
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
