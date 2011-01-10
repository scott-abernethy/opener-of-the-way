package code.gate

import java.util.Calendar
import code.comet._
import code.model._
import code.model.Mythos._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import net.liftweb.common._
import net.liftweb.squerylrecord.RecordTypeMode._

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)
case object LooseInterest

trait LurkerComponent {
  val lurker: Lurker
  trait Lurker extends Actor
}

trait LurkerComponentImpl extends LurkerComponent {
  this: FileSystemComponent =>
  val lurker = new Lurker with Loggable {
    def act() {
      while (true) {
        receive {
          case WayFound(g, lp) =>
            // how often do we check the fileSystem?
            val files = fileSystem.find(lp).toSet
            logger.info("WayFound " + files)
            val now = Calendar.getInstance
            Db.use{_ =>
              updateGate(g, x => x.state(GateState.open).localPath(lp))
              val existingFiles = g.artifacts.toList.map(_.path.is).toSet
              val (filesToUpdate, filesToAdd) = files partition(existingFiles contains _)
              // todo (when) do we remove missing files?
              filesToAdd.map(Artifact.createRecord.gatewayId(g.id).path(_).discovered(now).witnessed(now)).foreach( a =>  ArtifactServer ! ArtifactCreated(artifacts.insert(a)))
            }
          case WayLost(g) =>
            logger.info("WayLost")
            Db.use{_ =>
              updateGate(g, x => x.state(GateState.lost))
            }
          case LooseInterest => exit
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
}
