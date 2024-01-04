package top.saymzx.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import top.saymzx.easycontrol.app.entity.AppData;
import top.saymzx.easycontrol.app.entity.Device;

public class DbHelper extends SQLiteOpenHelper {

  private static final String dataBaseName = "app.db";
  private static final int version = 6;
  private final String tableName = "DevicesDb";

  public DbHelper(Context context) {
    super(context, dataBaseName, null, version);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + tableName + " (\n" + "\t uuid text PRIMARY KEY,\n" + "\t type integer,\n" + "\t name text,\n" + "\t address text,\n" + "\t isAudio integer,\n" + "\t maxSize integer,\n" + "\t maxFps integer,\n" + "\t maxVideoBit integer," + "\t setResolution integer," + "\t turnOffScreen integer," + "\t autoLockAfterControl integer," + "\t defaultFull integer," + "\t useH265 integer ," + "\t useOpus integer ," + "\t useTunnel integer ," + "\t window_x integer ," + "\t window_y integer ," + "\t window_width integer ," + "\t window_height integer " + ");");
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    // 检测表中是否存在window_x列
    try (Cursor cursor = db.rawQuery("select * from " + tableName + " limit 0", null)) {
      if (cursor.getColumnIndex("window_x") == -1) {
        db.execSQL("alter table " + tableName + " add column window_x integer");
        db.execSQL("alter table " + tableName + " add column window_y integer");
        db.execSQL("alter table " + tableName + " add column window_width integer");
        db.execSQL("alter table " + tableName + " add column window_height integer");
      }
    }
  }

  @SuppressLint("Range")
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 6) {
      // 获取旧数据
      ArrayList<Device> devices = getAll(db);
      // 修改表名
      db.execSQL("alter table " + tableName + " rename to tempTable");
      // 新建新表
      onCreate(db);
      // 将数据搬移至新表
      for (Device device : devices) insert(db, device);
      // 删除旧表
      db.execSQL("drop table tempTable");
    }
  }

  // 读取数据库设备列表
  @SuppressLint("Range")
  public ArrayList<Device> getAll() {
    return getAll(getReadableDatabase());
  }

  private ArrayList<Device> getAll(SQLiteDatabase db) {
    ArrayList<Device> devices = new ArrayList<>();
    try (Cursor cursor = db.query(tableName, null, null, null, null, null, null)) {
      if (cursor.moveToFirst()) do devices.add(getDeviceFormCursor(cursor)); while (cursor.moveToNext());
    }
    return devices;
  }

  // 查找
  @SuppressLint("Range")
  public Device getByUUID(String uuid) {
    Device device = null;
    try (Cursor cursor = getReadableDatabase().query(tableName, null, "uuid=?", new String[]{uuid}, null, null, null)) {
      if (cursor.moveToFirst()) device = getDeviceFormCursor(cursor);
    }
    return device;
  }

  // 更新
  public void insert(Device device) {
    insert(getWritableDatabase(), device);
  }

  public void insert(SQLiteDatabase db, Device device) {
    db.insert(tableName, null, getValues(device));
  }

  // 更新
  public void update(Device device) {
    getWritableDatabase().update(tableName, getValues(device), "uuid=?", new String[]{String.valueOf(device.uuid)});
  }

  // 删除
  public void delete(Device device) {
    getWritableDatabase().delete(tableName, "uuid=?", new String[]{String.valueOf(device.uuid)});
  }

  private ContentValues getValues(Device device) {
    ContentValues values = new ContentValues();
    values.put("uuid", device.uuid);
    values.put("type", device.type);
    values.put("name", device.name);
    values.put("isAudio", device.isAudio);
    values.put("address", device.address);
    values.put("maxSize", device.maxSize);
    values.put("maxFps", device.maxFps);
    values.put("maxVideoBit", device.maxVideoBit);
    values.put("setResolution", device.setResolution);
    values.put("turnOffScreen", device.turnOffScreen);
    values.put("autoLockAfterControl", device.autoLockAfterControl);
    values.put("defaultFull", device.defaultFull);
    values.put("useH265", device.useH265);
    values.put("useOpus", device.useOpus);
    values.put("useTunnel", device.useTunnel);
    values.put("window_x", device.window_x);
    values.put("window_y", device.window_y);
    values.put("window_width", device.window_width);
    values.put("window_height", device.window_height);
    return values;
  }

  @SuppressLint("Range")
  private Device getDeviceFormCursor(Cursor cursor) {
    return new Device(
      cursor.getString(cursor.getColumnIndex("uuid")),
      cursor.getInt(cursor.getColumnIndex("type")),
      cursor.getString(cursor.getColumnIndex("name")),
      cursor.getString(cursor.getColumnIndex("address")),
      cursor.getInt(cursor.getColumnIndex("isAudio")) == 1,
      cursor.getInt(cursor.getColumnIndex("maxSize")),
      cursor.getInt(cursor.getColumnIndex("maxFps")),
      cursor.getInt(cursor.getColumnIndex("maxVideoBit")),
      cursor.getInt(cursor.getColumnIndex("setResolution")) == 1,
      cursor.getInt(cursor.getColumnIndex("turnOffScreen")) == 1,
      cursor.getColumnIndex("autoLockAfterControl") == -1 ? AppData.setting.getDefaultAutoLockAfterControl() : cursor.getInt(cursor.getColumnIndex("autoLockAfterControl")) == 1,
      cursor.getInt(cursor.getColumnIndex("defaultFull")) == 1,
      cursor.getColumnIndex("useH265") == -1 ? AppData.setting.getDefaultUseH265() : cursor.getInt(cursor.getColumnIndex("useH265")) == 1,
      cursor.getColumnIndex("useOpus") == -1 ? AppData.setting.getDefaultUseOpus() : cursor.getInt(cursor.getColumnIndex("useOpus")) == 1,
      cursor.getColumnIndex("useTunnel") == -1 ? AppData.setting.getDefaultUseTunnel() : cursor.getInt(cursor.getColumnIndex("useTunnel")) == 1,
      cursor.getInt(cursor.getColumnIndex("window_x")),
      cursor.getInt(cursor.getColumnIndex("window_y")),
      cursor.getInt(cursor.getColumnIndex("window_width")),
      cursor.getInt(cursor.getColumnIndex("window_height"))
    );
  }
}
