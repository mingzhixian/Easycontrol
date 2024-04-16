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
  private static final int version = 23;
  private final String tableName = "DevicesDb";

  public DbHelper(Context context) {
    super(context, dataBaseName, null, version);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("CREATE TABLE " + tableName + " (");
    stringBuilder.append("uuid text PRIMARY KEY,");
    stringBuilder.append("type integer,");
    stringBuilder.append("name text,");
    stringBuilder.append("address text,");
    stringBuilder.append("startApp text,");
    stringBuilder.append("adbPort integer,");
    stringBuilder.append("serverPort integer,");
    stringBuilder.append("listenClip integer,");
    stringBuilder.append("isAudio integer,");
    stringBuilder.append("maxSize integer,");
    stringBuilder.append("maxFps integer,");
    stringBuilder.append("maxVideoBit integer,");
    stringBuilder.append("useH265 integer,");
    stringBuilder.append("connectOnStart integer,");
    stringBuilder.append("customResolutionOnConnect integer,");
    stringBuilder.append("wakeOnConnect integer,");
    stringBuilder.append("lightOffOnConnect integer,");
    stringBuilder.append("showNavBarOnConnect integer,");
    stringBuilder.append("changeToFullOnConnect integer,");
    stringBuilder.append("keepWakeOnRunning integer,");
    stringBuilder.append("changeResolutionOnRunning integer,");
    stringBuilder.append("smallToMiniOnRunning integer,");
    stringBuilder.append("fullToMiniOnRunning integer,");
    stringBuilder.append("miniTimeoutOnRunning integer,");
    stringBuilder.append("lockOnClose integer,");
    stringBuilder.append("lightOnClose integer,");
    stringBuilder.append("reconnectOnClose integer,");
    stringBuilder.append("customResolutionWidth integer,");
    stringBuilder.append("customResolutionHeight integer,");
    stringBuilder.append("smallX integer,");
    stringBuilder.append("smallY integer,");
    stringBuilder.append("smallLength integer,");
    stringBuilder.append("smallXLan integer,");
    stringBuilder.append("smallYLan integer,");
    stringBuilder.append("smallLengthLan integer,");
    stringBuilder.append("miniY integer);");
    db.execSQL(stringBuilder.toString());
  }

  @SuppressLint("Range")
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < version) {
      // 获取旧数据
      ArrayList<Device> devices = getAll(db);
      // 修改表名
      db.execSQL("alter table " + tableName + " rename to tempTable");
      // 新建新表
      onCreate(db);
      // 将数据搬移至新表
      for (Device device : devices) db.insert(tableName, null, getValues(device));
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
    getWritableDatabase().insert(tableName, null, getValues(device));
  }

  // 更新
  public void update(Device device) {
    getWritableDatabase().update(tableName, getValues(device), "uuid=?", new String[]{device.uuid});
  }

  // 删除
  public void delete(Device device) {
    getWritableDatabase().delete(tableName, "uuid=?", new String[]{device.uuid});
  }

  private ContentValues getValues(Device device) {
    ContentValues values = new ContentValues();
    values.put("uuid", device.uuid);
    values.put("type", device.type);
    values.put("name", device.name);
    values.put("address", device.address);
    values.put("startApp", device.startApp);
    values.put("adbPort", device.adbPort);
    values.put("serverPort", device.serverPort);
    values.put("listenClip", device.listenClip ? 1 : 0);
    values.put("isAudio", device.isAudio ? 1 : 0);
    values.put("maxSize", device.maxSize);
    values.put("maxFps", device.maxFps);
    values.put("maxVideoBit", device.maxVideoBit);
    values.put("useH265", device.useH265 ? 1 : 0);
    values.put("connectOnStart", device.connectOnStart ? 1 : 0);
    values.put("customResolutionOnConnect", device.customResolutionOnConnect ? 1 : 0);
    values.put("wakeOnConnect", device.wakeOnConnect ? 1 : 0);
    values.put("lightOffOnConnect", device.lightOffOnConnect ? 1 : 0);
    values.put("showNavBarOnConnect", device.showNavBarOnConnect ? 1 : 0);
    values.put("changeToFullOnConnect", device.changeToFullOnConnect ? 1 : 0);
    values.put("keepWakeOnRunning", device.keepWakeOnRunning ? 1 : 0);
    values.put("changeResolutionOnRunning", device.changeResolutionOnRunning ? 1 : 0);
    values.put("smallToMiniOnRunning", device.smallToMiniOnRunning ? 1 : 0);
    values.put("fullToMiniOnRunning", device.fullToMiniOnRunning ? 1 : 0);
    values.put("miniTimeoutOnRunning", device.miniTimeoutOnRunning ? 1 : 0);
    values.put("lockOnClose", device.lockOnClose ? 1 : 0);
    values.put("lightOnClose", device.lightOnClose ? 1 : 0);
    values.put("reconnectOnClose", device.reconnectOnClose ? 1 : 0);
    values.put("customResolutionWidth", device.customResolutionWidth);
    values.put("customResolutionHeight", device.customResolutionHeight);
    values.put("smallX", device.smallX);
    values.put("smallY", device.smallY);
    values.put("smallLength", device.smallLength);
    values.put("smallXLan", device.smallXLan);
    values.put("smallYLan", device.smallYLan);
    values.put("smallLengthLan", device.smallLengthLan);
    values.put("miniY", device.miniY);
    return values;
  }

  @SuppressLint("Range")
  private Device getDeviceFormCursor(Cursor cursor) {
    Device device = new Device(cursor.getString(cursor.getColumnIndex("uuid")), cursor.getInt(cursor.getColumnIndex("type")));
    for (int i = 0; i < cursor.getColumnCount(); i++) {
      switch (cursor.getColumnName(i)) {
        case "name": {
          device.name = cursor.getString(i);
          break;
        }
        case "address": {
          device.address = cursor.getString(i);
          break;
        }
        case "startApp": {
          device.startApp = cursor.getString(i);
          break;
        }
        case "adbPort": {
          device.adbPort = cursor.getInt(i);
          break;
        }
        case "serverPort": {
          device.serverPort = cursor.getInt(i);
          break;
        }
        case "listenClip": {
          device.listenClip = cursor.getInt(i) == 1;
          break;
        }
        case "isAudio": {
          device.isAudio = cursor.getInt(i) == 1;
          break;
        }
        case "maxSize": {
          device.maxSize = cursor.getInt(i);
          break;
        }
        case "maxFps": {
          device.maxFps = cursor.getInt(i);
          break;
        }
        case "maxVideoBit": {
          device.maxVideoBit = cursor.getInt(i);
          break;
        }
        case "useH265": {
          device.useH265 = cursor.getInt(i) == 1;
          break;
        }
        case "connectOnStart": {
          device.connectOnStart = cursor.getInt(i) == 1;
          break;
        }
        case "customResolutionOnConnect": {
          device.customResolutionOnConnect = cursor.getInt(i) == 1;
          break;
        }
        case "wakeOnConnect": {
          device.wakeOnConnect = cursor.getInt(i) == 1;
          break;
        }
        case "lightOffOnConnect": {
          device.lightOffOnConnect = cursor.getInt(i) == 1;
          break;
        }
        case "showNavBarOnConnect": {
          device.showNavBarOnConnect = cursor.getInt(i) == 1;
          break;
        }
        case "changeToFullOnConnect": {
          device.changeToFullOnConnect = cursor.getInt(i) == 1;
          break;
        }
        case "keepWakeOnRunning": {
          device.keepWakeOnRunning = cursor.getInt(i) == 1;
          break;
        }
        case "changeResolutionOnRunning": {
          device.changeResolutionOnRunning = cursor.getInt(i) == 1;
          break;
        }
        case "smallToMiniOnRunning": {
          device.smallToMiniOnRunning = cursor.getInt(i) == 1;
          break;
        }
        case "fullToMiniOnRunning": {
          device.fullToMiniOnRunning = cursor.getInt(i) == 1;
          break;
        }
        case "miniTimeoutOnRunning": {
          device.miniTimeoutOnRunning = cursor.getInt(i) == 1;
          break;
        }
        case "lockOnClose": {
          device.lockOnClose = cursor.getInt(i) == 1;
          break;
        }
        case "lightOnClose": {
          device.lightOnClose = cursor.getInt(i) == 1;
          break;
        }
        case "reconnectOnClose": {
          device.reconnectOnClose = cursor.getInt(i) == 1;
          break;
        }
        case "customResolutionWidth": {
          device.customResolutionWidth = cursor.getInt(i);
          break;
        }
        case "customResolutionHeight": {
          device.customResolutionHeight = cursor.getInt(i);
          break;
        }
        case "smallX": {
          device.smallX = cursor.getInt(i);
          break;
        }
        case "smallY": {
          device.smallY = cursor.getInt(i);
          break;
        }
        case "smallLength": {
          device.smallLength = cursor.getInt(i);
          break;
        }
        case "smallXLan": {
          device.smallXLan = cursor.getInt(i);
          break;
        }
        case "smallYLan": {
          device.smallYLan = cursor.getInt(i);
          break;
        }
        case "smallLengthLan": {
          device.smallLengthLan = cursor.getInt(i);
          break;
        }
        case "miniY": {
          device.miniY = cursor.getInt(i);
          break;
        }
      }
    }
    return device;
  }
}
