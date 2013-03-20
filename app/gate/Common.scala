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

package gate

import java.sql.Timestamp
import java.util.Calendar

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