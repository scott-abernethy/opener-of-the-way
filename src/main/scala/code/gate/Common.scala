package code.gate

import java.sql.Timestamp

case object Ping
case object Pong

object T {
  def now: Timestamp = new Timestamp(System.currentTimeMillis)
  def ago(period: Long): Timestamp = new Timestamp(System.currentTimeMillis - period)
  def zero: Timestamp = new Timestamp(0)
} 