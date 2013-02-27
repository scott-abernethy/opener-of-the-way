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
