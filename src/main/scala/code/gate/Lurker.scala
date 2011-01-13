package code.gate

import java.util.Calendar
import code.comet._
import code.model._
import code.model.Mythos._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._

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
            val now = new java.sql.Timestamp(new java.util.Date().getTime)
            transaction{
              updateGate(g, x => {
                x.state = GateState.open
                x.localPath = lp
                x
              })
              val existingFiles = g.artifacts.toList.map(_.path).toSet
              val (filesToUpdate, filesToAdd) = files partition(existingFiles contains _)
              // todo (when) do we remove missing files?
              filesToAdd.map(new Artifact(g.id, _, now, now)).foreach( a =>  ArtifactServer ! ArtifactCreated(artifacts.insert(a)))
            }
          case WayLost(g) =>
            logger.info("WayLost")
            transaction{
              updateGate(g, x => {
                x.state = GateState.lost
                x
              })
            }
          case LooseInterest => 
            exit
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
