package code.util

import java.text.DecimalFormat

object Size {
  // 1024s
  // K M G T P

  val units = List("", "K", "M", "G", "T", "P", "E", "Z", "Y")

  def short(length: Long): String = {
    short(length.toDouble, 0)
  }

  private def short(length: Double, power: Int): String = {
    if (length < 1000) {
      val rounded: Long = scala.math.round(length)
      if (rounded < 10)
      {
        if (power == 0)
        {
          rounded.toString + units(power)
        }
        else
        {
          // use unrounded and print with 1dp
          new DecimalFormat("0.0").format(length) + units(power)
        }
      }
      else
      {
        rounded.toString + units(power)
      }
    }
    else {
      short(length / 1024, power + 1)
    }
  }
}