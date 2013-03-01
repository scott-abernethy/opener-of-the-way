package util

import java.text.DecimalFormat
import model.Presence

object Size {
  // 1024s
  // K M G T P

  val units = List("B", "K", "M", "G", "T", "P", "E", "Z", "Y")
  val megaByteLength = 1024L * 1024;
  val gigaByteLength = megaByteLength * 1024;

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

  def total(lengths: List[Long]): Long = {
    lengths.foldLeft(0L)( (total,i) => total + i )
  }

  def gigs(count: Long): Long = {
    count * gigaByteLength
  }

  def megs(count: Long): Long = {
    count * megaByteLength
  }

  def printGiB(length: Long): String = {
    (length / gigaByteLength) + " GiB"
  }
}