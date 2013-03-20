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

import model._
import scala.util.matching.Regex
import org.squeryl.PrimitiveTypeMode._
import akka.actor.{ActorRef, Actor}
import concurrent.Await
import concurrent.duration._
import play.api.Logger

case class OpenGateway(gateway: Gateway)
case class CloseGateway(gateway: Gateway)
case class OpenGateSuccess(gateway: Gateway, localPath: String)
case class OpenGateFailed(gateway: Gateway)
case class GateFailed(gatewayId: Long)
case class CloseGateSuccess(gateway: Gateway)
case class CloseGateFailed(gateway: Gateway)

object Threshold {
  val localPathMessage = new Regex("""Gate opened[^']+'([^']+)'""")
}

/**
 * Merge with Keeper?
 */
class Threshold(processor: Processs) extends Actor {

  def receive = {

    case OpenGateway(g) => {
      val (future, destroy) = processor.start("threshold" :: "open" :: g.location :: g.path :: g.password :: Nil)
      // TODO don't block here, pipe
      val result = Await.result(future, 5 minutes)
      Logger.debug("Open " + g + " = " + result)
      Threshold.localPathMessage findFirstMatchIn (result.lines.flatten.mkString) map (_.group(1)) match {
        case Some(localPath) if (result.exitValue == 0) =>
          sender ! OpenGateSuccess(g, localPath)
        case _ =>
          sender ! OpenGateFailed(g)
      }
    }

    case CloseGateway(g) => {
      Logger.debug("Close " + g)
      val (future, destroy) = processor.start("threshold" :: "close" :: g.location :: g.path :: Nil)
      val result = Await.result(future, 5 minutes)
      if (result.exitValue == 0) {
        sender ! CloseGateSuccess(g)
      } else {
        sender ! CloseGateFailed(g)
      }
    }

  }
}