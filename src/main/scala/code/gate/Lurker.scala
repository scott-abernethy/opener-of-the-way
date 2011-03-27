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
import java.io.File

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)
case object LooseInterest

trait LurkerComponent {
  val lurker: Lurker
  trait Lurker extends Actor
}

trait LurkerComponentImpl extends LurkerComponent {
  this: FileSystemComponent with ManipulatorComponent =>
  val lurker = new Lurker with Loggable {
    def act() {
      loop {
        react {
          case WayFound(g, lp) =>
            logger.debug("WayFound " + lp)
            transaction{
              updateGate(g, x => {
                x.state = GateState.open
                x.localPath = lp
                x
              })
            }
            // how often do we check the fileSystem?
            val filesFound = g.mode match {
              case GateMode.source =>
                fileSystem.find(lp).filterNot(f => f.startsWith("/clones/") || f.startsWith("clones/")).toSet
              case _ =>
                Set.empty
            }
            val now = T.now
            transaction{
              // todo (when) do we remove missing files?
              filesFound.foreach {f =>
                val q = from(g.artifacts)(a => where(a.path === f) select(a))
                if (q.isEmpty) {
                  val created = artifacts.insert(new Artifact(g.id, f, now, now))
                  ArtifactServer ! ArtifactCreated(created)
                } else {
                  q.toList.foreach{a =>
                    a.witnessed = now
                    artifacts.update(a)
                  }
                }
              }
            }
            manipulator ! Wake
          case WayLost(g) =>
            logger.debug("WayLost")
            transaction{
              updateGate(g, x => {
                x.state = GateState.lost
                x
              })
            }
          case LooseInterest => 
            exit
          case Ping => reply(Pong)
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
