package code.util

object Size {
  // 1024s
  // K M G T P

  val units = List("", "K", "M", "G", "T", "P", "E", "Z", "Y")

  def short(length: Long): String = {
    short(length, 0)
  }

  private def short(length: Long, power: Int): String = {
    if (length < 1024) {
      length.toString + units(power)
    }
    else {
      short(length / 1024, power + 1)
    }
  }
}