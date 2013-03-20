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

import concurrent.Future

case class Exit(exitValue: Int, lines: Seq[String], duration: Long)

trait Destroyable {
  def destroy
}

class NonDestroyable extends Destroyable {
  def destroy {}
}

trait Processs {
  def start(cmds: Seq[String]): (Future[Exit], Destroyable)
}

object ProcesssImpl extends Processs {
  import sys.process._
  def start(cmds: Seq[String]): (Future[Exit], Destroyable) = {
    val startMsec = System.currentTimeMillis
    var lines: Seq[String] = Nil
    val process = cmds.run(ProcessLogger(line => lines = lines :+ line), false)
    val future = Future {
      val value = process.exitValue() // blocks until result
      Exit(value, lines, System.currentTimeMillis - startMsec)
    }(util.Context.ioOperations)
    val destroyable = new Destroyable {
      def destroy { process.destroy() }
    }
    (future, destroyable)
  }
}