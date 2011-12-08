package code.gate

import java.sql.Timestamp
import java.util.Calendar

case object Activate
case object Deactivate
case object Destroy

case object Ping
case object Pong

object T {
  def now: Timestamp = {
    new Timestamp(System.currentTimeMillis)
  }

  def ago(msecPeriod: Long): Timestamp = {
    new Timestamp(System.currentTimeMillis - msecPeriod)
  }

  def agoFrom(start: Timestamp, msecPeriod: Long): Timestamp = {
    new Timestamp(start.getTime - msecPeriod)
  }

  def yesterday: Timestamp = {
    ago(1000 * 60 * 60 * 24)
  }

  //  def zero: Timestamp = new Timestamp(0)

  def at(year: Int, month: Int, date: Int, hour: Int, minute: Int, second: Int): Timestamp = {
    val c = Calendar.getInstance()
    c.set(year, month, date, hour, minute, second)
    new Timestamp(c.getTimeInMillis)
  }

  def startOfDay(in: Timestamp): Timestamp = {
    val c = Calendar.getInstance()
    c.setTimeInMillis(in.getTime)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    new Timestamp(c.getTimeInMillis)
  }
}