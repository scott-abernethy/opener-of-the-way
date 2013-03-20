/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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