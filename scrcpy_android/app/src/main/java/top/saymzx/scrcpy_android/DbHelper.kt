package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
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
          "\t ip text,\n" +
          "\t name text PRIMARY KEY,\n" +
          "\t videoCodec text,\n" +
          "\t videoBit integer,\n" +
          "\t fps integer,\n" +
          "\t resolution integer)"
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
          db.execSQL("update DevicesDb set fps='30',videoBit='12000000' where name=='$name'")
        } while (cursor.moveToNext())
      }
      cursor.close()
    }
  }
}