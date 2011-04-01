package code.gate

import java.sql.Timestamp

case object Ping
case object Pong

object T {
  def now: Timestamp = new Timestamp(System.currentTimeMillis)
  def ago(msecPeriod: Long): Timestamp = new Timestamp(System.currentTimeMillis - msecPeriod)
  def yesterday: Timestamp = ago(1000 * 60 * 60 * 24)
  //  def zero: Timestamp = new Timestamp(0)
}