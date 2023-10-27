package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import top.saymzx.easycontrol.app.entity.Device;

public class DbHelper extends SQLiteOpenHelper {

  private static final String dataBaseName = "app.db";
  private static final int version = 1;
  private final String tableName = "DevicesDb";

  public DbHelper(Context context) {
    super(context, dataBaseName, null, version);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + tableName + " (\n" + "\t id integer PRIMARY KEY AUTOINCREMENT,\n" + "\t name text,\n" + "\t address text,\n" + "\t maxSize integer,\n" + "\t maxFps integer,\n" + "\t maxVideoBit integer," + "\t setResolution integer" + ")");
  }

  @SuppressLint("Range")
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // 处理数据库升级逻辑
  }

  // 读取数据库设备列表
  @SuppressLint("Range")
  public ArrayList<Device> getAll() {
    ArrayList<Device> devices = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query(tableName, null, null, null, null, null, null)) {
      if (cursor.moveToFirst())
        do devices.add(getDeviceFormCursor(cursor)); while (cursor.moveToNext());
    }
    return devices;
  }

  // 查找
  @SuppressLint("Range")
  public Device getById(int id) {
    Device device = null;
    try (Cursor cursor = getReadableDatabase().query(tableName, null, "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
      if (cursor.moveToFirst()) device = getDeviceFormCursor(cursor);
    }
    return device;
  }

  // 更新
  public void insert(Device device) {
    getWritableDatabase().insert(tableName, null, getValues(device));
  }

  // 更新
  public void update(Device device) {
    getWritableDatabase().update(tableName, getValues(device), "id=?", new String[]{String.valueOf(device.id)});
  }

  // 删除
  public void delete(Device device) {
    getWritableDatabase().delete(tableName, "id=?", new String[]{String.valueOf(device.id)});
  }

  private ContentValues getValues(Device device) {
    ContentValues values = new ContentValues();
    values.put("id", device.id);
    values.put("name", device.name);
    values.put("address", device.address);
    values.put("maxSize", device.maxSize);
    values.put("maxFps", device.maxFps);
    values.put("maxVideoBit", device.maxVideoBit);
    values.put("setResolution", device.setResolution);
    return values;
  }

  @SuppressLint("Range")
  private Device getDeviceFormCursor(Cursor cursor) {
    return new Device(
      cursor.getInt(cursor.getColumnIndex("id")),
      cursor.getString(cursor.getColumnIndex("name")),
      cursor.getString(cursor.getColumnIndex("address")),
      cursor.getInt(cursor.getColumnIndex("maxSize")),
      cursor.getInt(cursor.getColumnIndex("maxFps")),
      cursor.getInt(cursor.getColumnIndex("maxVideoBit")),
      cursor.getInt(cursor.getColumnIndex("setResolution")) == 1);
  }
}
