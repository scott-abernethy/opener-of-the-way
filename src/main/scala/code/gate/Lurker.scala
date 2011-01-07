package code.gate

import java.util.Calendar
import code.model._
import code.model.Mythos._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import net.liftweb.squerylrecord.RecordTypeMode._

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)

class Lurker(fileSystem: FileSystem) extends Actor {
  def act() {
    while (true) {
      receive {
        case WayFound(g, lp) =>
          // how often do we check the fileSystem?
          val files = fileSystem.list(lp)
          val now = Calendar.getInstance
          Db.use{_ =>
            updateGate(g, x => x.state(GateState.open).localPath(lp))
            artifacts.insert(files.map(Artifact.createRecord.gatewayId(g.id).path(_).discovered(now).witnessed(now)))
          }
        case WayLost(g) =>
          Db.use{_ =>
            updateGate(g, x => x.state(GateState.lost))
          }
        case _ =>
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
