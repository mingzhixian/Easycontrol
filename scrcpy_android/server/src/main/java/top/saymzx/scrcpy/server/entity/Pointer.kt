package top.saymzx.scrcpy.server.entity

class Pointer(
  val id: Int,
  val localId: Int
) {
  var x = 0f
  var y = 0f
  var pressure = 0f
  var isUp = false
}