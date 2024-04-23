package top.saymzx.easycontrol.app.helper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import top.saymzx.easycontrol.app.DeviceDetailActivity;
import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.client.Client;
import top.saymzx.easycontrol.app.client.tools.AdbTools;
import top.saymzx.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.saymzx.easycontrol.app.databinding.ItemLoadingBinding;
import top.saymzx.easycontrol.app.databinding.ItemSetDeviceBinding;
import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  private final Context context;
  private Device sendFileDevice;
  private static final int[] colors = {R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color5, R.color.color6, R.color.color7, R.color.color8, R.color.color9};
  private static final int[] onColors = {R.color.onColor1, R.color.onColor2, R.color.onColor3, R.color.onColor4, R.color.onColor5, R.color.onColor6, R.color.onColor7, R.color.onColor8, R.color.onColor9};

  // 生成随机数
  private final Random random = new Random();

  public DeviceListAdapter(Context c) {
    queryDevices();
    context = c;
  }

  @Override
  public int getCount() {
    return AdbTools.devicesList.size();
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
    Device device = AdbTools.devicesList.get(i);
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    int colorInt = random.nextInt(8);
    devicesItemBinding.deviceIconBackground.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(colors[colorInt])));
    devicesItemBinding.deviceIcon.setImageTintList(ColorStateList.valueOf(context.getResources().getColor(onColors[colorInt])));
    devicesItemBinding.deviceIcon.setImageResource(device.isNetworkDevice() ? R.drawable.wifi : R.drawable.link);
    devicesItemBinding.deviceType.setText(context.getString(device.isNetworkDevice() ? R.string.main_device_type_network : R.string.main_device_type_link));
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> Client.startDevice(device));
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
    itemSetDeviceBinding.buttonStartWireless.setVisibility(device.isLinkDevice() ? View.VISIBLE : View.GONE);
    // 设置监听
    itemSetDeviceBinding.buttonStartWireless.setOnClickListener(v -> {
      dialog.cancel();
      restartWireless(device);
    });
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      reset(device);
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
    itemSetDeviceBinding.buttonPushFile.setOnClickListener(v -> {
      dialog.cancel();
      sendFileDevice = device;
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.setType("*/*");
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
      ((Activity) context).startActivityForResult(intent, 1);
    });
  }

  private void queryDevices() {
    ArrayList<Device> rawDevices = AppData.dbHelper.getAll();
    ArrayList<Device> tmp1 = new ArrayList<>();
    ArrayList<Device> tmp2 = new ArrayList<>();
    for (Device device : rawDevices) {
      if (device.isLinkDevice() && AdbTools.usbDevicesList.containsKey(device.address)) tmp1.add(device);
      else if (device.isNetworkDevice()) tmp2.add(device);
    }
    AdbTools.devicesList.clear();
    AdbTools.devicesList.addAll(tmp1);
    AdbTools.devicesList.addAll(tmp2);
  }

  private void restartWireless(Device device) {
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.restartOnTcpip(device, result -> AppData.uiHandler.post(() -> {
      loading.second.cancel();
      Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show();
    }));
  }

  private void reset(Device device) {
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.runOnceCmd(device, "wm size reset", result -> AppData.uiHandler.post(() -> {
      loading.second.cancel();
      Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show();
    }));
  }

  public void pushFile(InputStream inputStream, String fileName) {
    if (inputStream == null) return;
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.pushFile(sendFileDevice, inputStream, fileName, process -> AppData.uiHandler.post(() -> {
      if (process < 0) {
        loading.second.cancel();
        Toast.makeText(context, context.getString(R.string.toast_fail), Toast.LENGTH_SHORT).show();
      } else if (process == 100) {
        loading.second.cancel();
        Toast.makeText(context, context.getString(R.string.toast_success), Toast.LENGTH_SHORT).show();
      } else loading.first.text.setText(process + " %");
    }));
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
