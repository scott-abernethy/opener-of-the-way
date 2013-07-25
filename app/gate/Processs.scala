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

import concurrent._
import _root_.util.Context
import scala.util.Random

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

object ProcesssFactory {
  import play.api.Play

  def create(): Processs = {
    import play.api.Play.current
    if (Play.isProd) ProcesssImpl
    else DevProcesssImpl
  }
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
    }(Context.ioOperations)
    val destroyable = new Destroyable {
      def destroy { process.destroy() }
    }
    (future, destroyable)
  }
}

object DevProcesssImpl extends Processs {
  def start(cmds: Seq[String]): (Future[Exit], Destroyable) = {
    // Setup handles to return
    val promise = Promise[Exit]()
    val destroyer = new Destroyable {
      def destroy { promise.failure(new InterruptedException()) }
    }
    
    // Do work async in separate execution context
    future {
      cmds match {
        case "threshold" :: "open" :: _ => {
          Thread.sleep(1000)
          promise.success(Exit(0, List("Note: Dev processs implementation, fake response", "Input was: " + cmds.mkString(" "), "Gate opened on '/tmp/fake/directory%d'".format(Random.nextInt(10000)) ), 1000))
        }
        case "threshold" :: "close" :: _ => {
          Thread.sleep(1000)
          promise.success(Exit(0, List("Note: Dev processs implementation, fake response", "Input was: " + cmds.mkString(" "), "Gate closed"), 1000))
        }
        case "cloner" :: _ | "presenter" :: _ => {
          // Could make a percentage of these fail...
          val duration = (5 + Random.nextInt(30)) * 1000
          Thread.sleep(duration)
          promise.success(Exit(0, List("Note: Dev processs implementation, fake response", "Input was: " + cmds.mkString(" "), "Job done"), duration))
        }
        case other => {
          Thread.sleep(1000)
          promise.success(Exit(-1, List("Note: Dev processs implementation, fake response", "Input was: " + cmds.mkString(" "), "Unknown command!"), 1000))
        }
      }
    }(Context.ioOperations)
    
    (promise.future, destroyer)
  }
}