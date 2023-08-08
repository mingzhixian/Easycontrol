package top.saymzx.scrcpy.server.entity

class DisplayInfo(
  val displayId: Int,
  val size: Pair<Int, Int>,
  val rotation: Int,
  val layerStack: Int,
  val flags: Int
) {

  companion object {
    const val FLAG_SUPPORTS_PROTECTED_BUFFERS = 0x00000001
  }
}