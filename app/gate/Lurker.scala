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

import java.util.Calendar
import model._
import model.Mythos._
import scala.collection.JavaConversions._
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import state.{ArtifactCreated, ArtifactTouched, ArtifactServer}
import akka.event.Logging
import akka.actor.{ActorRef, Actor}
import play.api.Logger
import java.sql.Timestamp
import state.FlushAllGateways
import state.ChangedGateway

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)
case class AddArtifact(gatewayId: Long, path: String, length: Long, found: Timestamp)

class Lurker(val manipulator: ActorRef, val artifactServer: ActorRef, val gatewayServer: ActorRef) extends Actor {

  var watcher: ActorRef = context.system.deadLetters

  override def preStart() {
    super.preStart()
    watcher = context.system.actorFor("/user/Watcher")
  }

  def receive = {
          case WayFound(g, lp) =>
            if (shouldScour(g)) scourGateway(g, lp)
            // TODO no need to chain this, move manipulator call external
            manipulator ! 'Wake

          case 'Flush =>
            // TODO do this in Watcher instead.
            transaction ( update(gateways)(g =>
              where(g.state === GateState.open)
              set(g.state := GateState.closed)
            ) )
            gatewayServer ! FlushAllGateways

          case AddArtifact(gatewayId, path, length, found) => {
            addArtifact(gatewayId, path, length, found)
          }

          case Ping =>
            sender ! Pong

          case _ =>
    }

    private def shouldScour(g: Gateway): Boolean = {
      transaction(gateways.lookup(g.id)) match {
        case Some(g2) =>
          g2.source == true && (g2.scoured.before(T.ago(Gateway.scourPeriod)) || g2.scourAsap)
        case _ =>
          false
      }
    }

    private def scourGateway(g: Gateway, lp: String): Unit = {
      Logger.debug("WayScoured " + lp)
      val now = T.now
      val filesFound = FileSystemImpl.find(lp).filterNot{ x =>
        val lower = x._1.toLowerCase
        lower.matches("^/?(ignored/|clones/|system volume information/|recycled/|readme|\\..+|\\$.+).*") ||
        lower.matches("^.+\\.(nfo|nzb|par2|sfv|srr|srs)(\\.[0-9]+)?$") ||
        lower.matches("^.+(\\.ds_store)$")
      }
//        _._1.matches("^/?(ignored/|clones/|System Volume Information/|Recycled/|README|\\..+|\\$.+).*")).filterNot(_._1.matches("^.+(nfo/|nzb/|System Volume Information/|Recycled/|README|\\..+|\\$.+).*"))
      // TODO (when) do we remove missing files?
      filesFound.foreach { i =>
        addArtifact(g.id, i._1, i._2, now)
      }
      transaction {
        // TODO do this in Watcher instead?
        update(gateways)(x =>
          where(x.id === g.id)
          set(x.scoured := now, x.scourAsap := false)
        )
      }
      watcher ! 'Wake
      gatewayServer ! ChangedGateway(g.id, g.cultistId)
    }


  def addArtifact(gatewayId: Long, path: String, length: Long, found: Timestamp) {
    transaction {
      Artifact.findUnique(gatewayId, path) match {
        case Some(a) => {
          a.witnessed = found
          a.length = length
          artifacts.update(a)
          // TODO update ArtifactServer?
        }
        case None => {
          val a = new Artifact
          a.gatewayId = gatewayId
          a.path = path
          a.discovered = found
          a.witnessed = found
          a.length = length
          artifactServer ! ArtifactTouched(ArtifactCreated, artifacts.insert(a).id)
        }
      }
    }
  }

  private def updateGate(g: Gateway, updater: (Gateway) => Gateway) {
      gateways.lookup(g.id) match {
        case Some(x) => gateways.update(updater(x))
        case _ => // oop
      }
    }
}
