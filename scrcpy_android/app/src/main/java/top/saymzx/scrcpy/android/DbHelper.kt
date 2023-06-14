package top.saymzx.scrcpy.android

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(
  context: Context?,
  name: String?,
  version: Int
) : SQLiteOpenHelper(context, name, null, version) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE DevicesDb (\n" +
          "\t name text PRIMARY KEY,\n" +
          "\t address text,\n" +
          "\t port integer,\n" +
          "\t videoCodec text,\n" +
          "\t maxSize integer,\n" +
          "\t fps integer,\n" +
          "\t videoBit integer," +
          "\t setResolution integer," +
          "\t defaultFull integer," +
          "\t floatNav integer" +
          ")"
    )
  }

  @SuppressLint("Range")
  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) {
      // 新建列
      db!!.execSQL("alter table DevicesDb add column fps integer")
      db.execSQL("alter table DevicesDb add column videoBit integer")
      // 更新旧数据的默认值
      val cursor =
        db.query("DevicesDb", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val name = cursor.getString(cursor.getColumnIndex("name"))
          db.execSQL("update DevicesDb set fps='60',videoBit='16000000' where name=='$name'")
        } while (cursor.moveToNext())
      }
      cursor.close()
    }
    // 修改列名，增加端口列
    if (oldVersion < 3) {
      // 修改表名
      db!!.execSQL("alter table DevicesDb rename to DevicesDbOld")
      // 新建新表
      db.execSQL(
        "CREATE TABLE DevicesDb (\n" +
            "\t name text PRIMARY KEY,\n" +
            "\t address text,\n" +
            "\t port integer,\n" +
            "\t videoCodec text,\n" +
            "\t resolution integer,\n" +
            "\t fps integer,\n" +
            "\t videoBit integer)"
      )
      // 将数据搬移至新表
      val cursor =
        db.query("DevicesDbOld", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("ip")))
            put("port", 5555)
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            put("resolution", cursor.getString(cursor.getColumnIndex("resolution")))
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            put("videoBit", cursor.getString(cursor.getColumnIndex("videoBit")))
          }
          db.insert("DevicesDb", null, values)
        } while (cursor.moveToNext())
      }
      cursor.close()
      // 删除旧表
      db.execSQL("drop table DevicesDbOld")
    }
    // 修改列名，增加默认全屏列、是否修改分辨率列
    if (oldVersion < 4) {
      // 修改表名
      db!!.execSQL("alter table DevicesDb rename to DevicesDbOld")
      // 新建新表
      db.execSQL(
        "CREATE TABLE DevicesDb (\n" +
            "\t name text PRIMARY KEY,\n" +
            "\t address text,\n" +
            "\t port integer,\n" +
            "\t videoCodec text,\n" +
            "\t maxSize integer,\n" +
            "\t fps integer,\n" +
            "\t videoBit integer," +
            "\t setResolution integer," +
            "\t defaultFull integer" +
            ")"
      )
      // 将数据搬移至新表
      val cursor =
        db.query("DevicesDbOld", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("address")))
            put("port", cursor.getInt(cursor.getColumnIndex("port")))
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            // 修改为默认值
            put("maxSize", "1600")
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            // 修改为默认值
            put("videoBit", "8000000")
            put("setResolution", 1)
            put("defaultFull", 1)
          }
          db.insert("DevicesDb", null, values)
        } while (cursor.moveToNext())
      }
      cursor.close()
      // 删除旧表
      db.execSQL("drop table DevicesDbOld")
    }
    // 修改列名，增加是否显示导航球列
    if (oldVersion < 5) {
      // 修改表名
      db!!.execSQL("alter table DevicesDb rename to DevicesDbOld")
      // 新建新表
      db.execSQL(
        "CREATE TABLE DevicesDb (\n" +
            "\t name text PRIMARY KEY,\n" +
            "\t address text,\n" +
            "\t port integer,\n" +
            "\t videoCodec text,\n" +
            "\t maxSize integer,\n" +
            "\t fps integer,\n" +
            "\t videoBit integer," +
            "\t setResolution integer," +
            "\t defaultFull integer," +
            "\t floatNav integer" +
            ")"
      )
      // 将数据搬移至新表
      val cursor =
        db.query("DevicesDbOld", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("address")))
            put("port", cursor.getInt(cursor.getColumnIndex("port")))
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            // 修改为默认值
            put("maxSize", "1600")
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            // 修改为默认值
            put("videoBit", "8000000")
            put("setResolution", 1)
            put("defaultFull", 1)
            put("floatNav", 1)
          }
          db.insert("DevicesDb", null, values)
        } while (cursor.moveToNext())
      }
      cursor.close()
      // 删除旧表
      db.execSQL("drop table DevicesDbOld")
    }
    // 修改列名，增加是否设置音频放大器
    if (oldVersion < 6) {
      // 修改表名
      db!!.execSQL("alter table DevicesDb rename to DevicesDbOld")
      // 新建新表
      db.execSQL(
        "CREATE TABLE DevicesDb (\n" +
            "\t name text PRIMARY KEY,\n" +
            "\t address text,\n" +
            "\t port integer,\n" +
            "\t videoCodec text,\n" +
            "\t maxSize integer,\n" +
            "\t fps integer,\n" +
            "\t videoBit integer," +
            "\t setResolution integer," +
            "\t defaultFull integer," +
            "\t floatNav integer," +
            "\t setLoud integer" +
            ")"
      )
      // 将数据搬移至新表
      val cursor =
        db.query("DevicesDbOld", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("address")))
            put("port", cursor.getInt(cursor.getColumnIndex("port")))
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            put("maxSize", cursor.getString(cursor.getColumnIndex("maxSize")))
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            put("videoBit", cursor.getString(cursor.getColumnIndex("videoBit")))
            put("setResolution", cursor.getString(cursor.getColumnIndex("setResolution")))
            put("defaultFull", cursor.getString(cursor.getColumnIndex("defaultFull")))
            put("floatNav", cursor.getString(cursor.getColumnIndex("floatNav")))
            put("setLoud", 0)
          }
          db.insert("DevicesDb", null, values)
        } while (cursor.moveToNext())
      }
      cursor.close()
      // 删除旧表
      db.execSQL("drop table DevicesDbOld")
    }
    // 修改默认值
    if (oldVersion < 7) {
      val cursor =
        db!!.query("DevicesDb", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("address")))
            put("port", cursor.getInt(cursor.getColumnIndex("port")))
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            put("maxSize", cursor.getString(cursor.getColumnIndex("maxSize")))
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            put("videoBit", cursor.getString(cursor.getColumnIndex("videoBit")))
            put("setResolution", cursor.getString(cursor.getColumnIndex("setResolution")))
            put("defaultFull", cursor.getString(cursor.getColumnIndex("defaultFull")))
            put("floatNav", cursor.getString(cursor.getColumnIndex("floatNav")))
            put("setLoud", 1)
          }
          db.update(
            "DevicesDb",
            values,
            "name=?",
            arrayOf(cursor.getString(cursor.getColumnIndex("name")))
          )
        } while (cursor.moveToNext())
      }
      cursor.close()
    }
    // 删除列
    if (oldVersion < 8) {
      val cursor =
        db!!.query("DevicesDb", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("address")))
            put("port", cursor.getInt(cursor.getColumnIndex("port")))
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            put("maxSize", cursor.getString(cursor.getColumnIndex("maxSize")))
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            put("videoBit", cursor.getString(cursor.getColumnIndex("videoBit")))
            put("setResolution", cursor.getString(cursor.getColumnIndex("setResolution")))
            put("defaultFull", cursor.getString(cursor.getColumnIndex("defaultFull")))
            put("floatNav", cursor.getString(cursor.getColumnIndex("floatNav")))
          }
          db.update(
            "DevicesDb",
            values,
            "name=?",
            arrayOf(cursor.getString(cursor.getColumnIndex("name")))
          )
        } while (cursor.moveToNext())
      }
      cursor.close()
    }
  }
}