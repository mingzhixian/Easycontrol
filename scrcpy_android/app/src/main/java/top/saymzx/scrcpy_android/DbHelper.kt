package top.saymzx.scrcpy_android

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
          "\t resolution integer)"
    )
  }

  override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    // 暂不需要
  }
}