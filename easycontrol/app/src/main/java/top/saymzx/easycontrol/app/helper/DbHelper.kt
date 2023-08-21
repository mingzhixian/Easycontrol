package top.saymzx.easycontrol.app.helper

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import top.saymzx.easycontrol.app.entity.Device

@Database(entities = [Device::class], version = 1)
abstract class SQLDatabase : RoomDatabase() {
  abstract fun devices(): DeviceDao
}

@Dao
interface DeviceDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(vararg device: Device)

  @Update
  fun updateUsers(vararg device: Device)

  @Delete
  fun delete(device: Device)

  @Query("SELECT * FROM Device")
  fun getAll(): List<Device>

  @Query("SELECT * FROM Device WHERE id=:id")
  fun getById(id: Int): List<Device>
}