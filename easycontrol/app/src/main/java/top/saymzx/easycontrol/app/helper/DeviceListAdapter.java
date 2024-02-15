package top.saymzx.easycontrol.app.helper;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

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
    setView(view, device);
    return view;
  }

  // 创建View
  private void setView(View view, Device device) {
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    devicesItemBinding.deviceIcon.setImageResource(device.isLinkDevice() ? R.drawable.link : R.drawable.wifi);
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> startDevice(device, AppData.setting.getChangeToFullOnConnect()));
    devicesItemBinding.buttonSmall.setOnClickListener(v -> startDevice(device, false));
    devicesItemBinding.buttonFull.setOnClickListener(v -> startDevice(device, true));
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
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
        ClientStream.restartOnTcpip(device, usbDevice, result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.set_device_button_start_wireless_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
      });
    } else itemSetDeviceBinding.buttonStartWireless.setVisibility(View.GONE);
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      if (device.isLinkDevice()) {
        UsbDevice usbDevice = linkDevices.get(device.uuid);
        if (usbDevice == null) return;
        ClientStream.runOnceCmd(device, usbDevice, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.set_device_button_recover_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
      } else ClientStream.runOnceCmd(device, null, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(context, context.getString(result ? R.string.set_device_button_recover_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
    });
    itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
      dialog.cancel();
      if (!device.isNormalDevice()) return;
      AppData.setting.setDefaultDevice(device.uuid);
    });
    itemSetDeviceBinding.buttonGetUuid.setOnClickListener(v -> {
      dialog.cancel();
      AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, device.uuid));
      Toast.makeText(context, context.getString(R.string.set_device_button_get_uuid_success), Toast.LENGTH_SHORT).show();
    });
    itemSetDeviceBinding.buttonChange.setOnClickListener(v -> {
      dialog.cancel();
      ViewTools.createDeviceDetailView(context, device, this).show();
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
      else if (device.isNormalDevice()) tmp2.add(device);
    }
    devicesList.clear();
    devicesList.addAll(tmp1);
    devicesList.addAll(tmp2);
  }

  public void startByUUID(String uuid) {
    for (Device device : devicesList) if (Objects.equals(device.uuid, uuid)) startDevice(device, AppData.setting.getChangeToFullOnConnect());
  }

  public void startDevice(Device device, boolean changeToFullOnConnect) {
    device.changeToFullOnConnect = changeToFullOnConnect;
    if (device.isLinkDevice()) {
      UsbDevice usbDevice = linkDevices.get(device.uuid);
      if (usbDevice == null) return;
      new Client(device, usbDevice);
    } else new Client(device);
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
