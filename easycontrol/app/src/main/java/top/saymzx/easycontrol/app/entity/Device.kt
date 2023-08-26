package top.saymzx.easycontrol.app.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Device(
  @PrimaryKey(autoGenerate = true)
  var id: Int? = null,
  var name: String,
  var address: String,
  var port: Int,
  var videoCodec: String,
  var audioCodec: String,
  var maxSize: Int,
  var maxFps: Int,
  var maxVideoBit: Int,
  var setResolution: Boolean
)