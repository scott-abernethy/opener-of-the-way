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
import code.util.ExceptionLoggingActor

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)
case object LooseInterest

trait LurkerComponent {
  val lurker: Lurker
  trait Lurker extends ExceptionLoggingActor
}

trait LurkerComponentImpl extends LurkerComponent {
  this: FileSystemComponent with ManipulatorComponent =>
  val lurker = new Lurker with Loggable {
    def act() {
      loop {
        react {
          case WayFound(g, lp) =>
            logger.debug("WayFound " + g)
            markGatewayOpen(g, lp)
            if (shouldScour(g)) scourGateway(g, lp)
            manipulator ! Wake
          case WayLost(g) =>
            logger.debug("WayLost " + g)
            transaction{
              updateGate(g, x => {
                x.state = if (g.seen.before(T.ago(4*24*60*60*1000))) GateState.lost else GateState.inactive
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
    private def shouldScour(g: Gateway): Boolean = {
      transaction(gateways.lookup(g.id)) match {
        case Some(g2) =>
          g2.mode == GateMode.source && g2.scoured.before(T.ago(3*60*60*1000L)) // 3 hours
        case _ =>
          false
      }
    }
    private def markGatewayOpen(g: Gateway, lp: String) {
      transaction {
        updateGate(g, x => {
          x.seen = T.now
          x.state = GateState.open
          x.localPath = lp
          x
        })
      }
    }
    private def scourGateway(g: Gateway, lp: String): Unit = {
      logger.debug("WayScoured " + lp)
      val now = T.now
      val filesFound = fileSystem.find(lp).filterNot(_.matches("^/?(clones|System Volume Information|Recycled)/.*")).toSet
      transaction {
        updateGate(g, x => {
          x.scoured = now
          x
        })
        // todo (when) do we remove missing files?
        filesFound.foreach {
          f =>
            val q = from(g.artifacts)(a => where(a.path === f) select (a))
            if (q.isEmpty) {
              val created = artifacts.insert(new Artifact(g.id, f, now, now))
              ArtifactServer ! ArtifactCreated(created)
            } else {
              q.toList.foreach {
                a =>
                  a.witnessed = now
                  artifacts.update(a)
              }
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
}
