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

  def futureFrom(start: Timestamp, msecPeriod: Long): Timestamp = {
    new Timestamp(start.getTime + msecPeriod)
  }

  def future(msecPeriod: Long): Timestamp = {
    new Timestamp(System.currentTimeMillis + msecPeriod)
  }

  def yesterday: Timestamp = {
    ago(Millis.days(1))
  }

  def tomorrow: Timestamp = {
    future(Millis.days(1))
  }

  def at(year: Int, month: Int, date: Int, hour: Int, minute: Int, second: Int): Timestamp = {
    val c = Calendar.getInstance()
    c.set(year, month, date, hour, minute, second)
    new Timestamp(c.getTimeInMillis)
  }

  def startOfDay(): Timestamp = {
    startOfDay(now)
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

  def startOfSevenDayPeriod(): Timestamp = {
    T.startOfDay(T.ago(Millis.days(7)))
  }
}

object Millis {
  def days(count: Int): Long = {
    count * hours(24)
  }

  def hours(count: Int): Long = {
    count * minutes(60)
  }

  def minutes(count: Int): Long = {
    count * seconds(60)
  }

  def seconds(count: Int): Long = {
    count * 1000L
  }

}