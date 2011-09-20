package code.gate

import java.sql.Timestamp
import java.util.Calendar

case object Activate
case object Deactivate
case object Destroy

case object Ping
case object Pong

object T {
  def now: Timestamp = new Timestamp(System.currentTimeMillis)
  def ago(msecPeriod: Long): Timestamp = new Timestamp(System.currentTimeMillis - msecPeriod)
  def agoFrom(start: Timestamp, msecPeriod: Long): Timestamp = new Timestamp(start.getTime - msecPeriod)
  def yesterday: Timestamp = ago(1000 * 60 * 60 * 24)
  //  def zero: Timestamp = new Timestamp(0)
  def at(year: Int, month: Int, date: Int, hour: Int, minute: Int, second: Int): Timestamp = {
    val c = Calendar.getInstance()
    c.set(year, month, date, hour, minute, second)
    new Timestamp(c.getTime.getTime)
  }
}