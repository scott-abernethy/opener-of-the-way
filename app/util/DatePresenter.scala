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

import java.text._
import java.util.Date
import java.util.TimeZone


object DatePresentation {
  def ago(begin: Long): String = ago(begin, System.currentTimeMillis)
  def ago(begin: Long, end: Long): String = duration(end - begin) + " ago"
  def duration(time: Long): String = {
    val mins = time / (60 * 1000)
    val hours = mins / 60L
    val days = hours / 24L
    val years = days / 365L

    if (years > 0) return format(years, "year")
    if (days > 0) return format(days, "day")
    if (hours > 0) return format(hours, "hour")
    if (mins > 0) return format(mins, "min")
    if (time > 0) return "<1 min"
    "?"
  }
  def format(count: Long, word: String) = count + " " + word + (if (count != 1) "s" else "")

  val atAbbreviation = formatTime("MMM-dd',' HH:mm") _

  val yearMonthDay = formatTime("yyyy-MM-dd") _

  def formatTime(formatPattern: String)(time: Long): String = {
    val f = new SimpleDateFormat(formatPattern)
    f.setTimeZone(TimeZone.getDefault)
    f.format(new java.util.Date(time))
  }
}
