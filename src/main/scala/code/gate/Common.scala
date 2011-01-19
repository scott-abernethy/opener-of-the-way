package code.gate

import java.sql.Timestamp

case object Ping
case object Pong

object T {
  def now: Timestamp = new Timestamp(System.currentTimeMillis)
} 