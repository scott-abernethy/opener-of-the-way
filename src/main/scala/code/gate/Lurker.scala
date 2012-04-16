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
            if (shouldScour(g)) scourGateway(g, lp)
            // TODO no need to chain this, move manipulator call external
            manipulator ! Wake

          case 'Flush =>
            // TODO do this in Watcher instead.
            transaction ( update(gateways)(g =>
              where(g.state === GateState.open)
              set(g.state := GateState.closed)
            ) )
            GatewayServer ! 'Flush

          case LooseInterest =>
            exit

          case Ping =>
            reply(Pong)

          case _ =>
        }
      }
    }

    private def shouldScour(g: Gateway): Boolean = {
      transaction(gateways.lookup(g.id)) match {
        case Some(g2) =>
          g2.source == true && g2.scoured.before(T.ago(Gateway.scourPeriod))
        case _ =>
          false
      }
    }

    private def scourGateway(g: Gateway, lp: String): Unit = {
      logger.debug("WayScoured " + lp)
      val now = T.now
      val filesFound = fileSystem.find(lp).filterNot(_._1.matches("^/?(ignored/|clones/|System Volume Information/|Recycled/|README|\\..+|\\$.+).*"))
      // TODO (when) do we remove missing files?
      filesFound.foreach { i =>
        val path = i._1
        val length = i._2
        transaction {
          from(g.artifacts)(a => where(a.path === path) select(a)).headOption match {
            case Some(a) => {
              a.witnessed = now
              a.length = length
              artifacts.update(a)
              // TODO update ArtifactServer?
            }
            case None => {
              val a = new Artifact
              a.gatewayId = g.id
              a.path = path
              a.discovered = now
              a.witnessed = now
              a.length = length
              ArtifactServer ! ArtifactTouched(ArtifactCreated, artifacts.insert(a).id)
            }
          }
        }
      }
      transaction {
        // TODO do this in Watcher instead?
        update(gateways)(x =>
          where(x.id === g.id)
          set(x.scoured := now)
        )
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
