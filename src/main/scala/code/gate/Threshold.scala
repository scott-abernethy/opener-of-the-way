package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.util.{Helpers, ActorPing}
import java.util.concurrent.ScheduledFuture
import net.liftweb.actor.LiftActor

case class Open()
case class Close()
case class Maintain()
case object Destroy

object Threshold {
  val localPathMessage = new Regex("""Gate opened[^']+'([^']+)'""")
}

class Threshold(gateway: Gateway, lurker: Actor, processor: Processor) extends Actor with Loggable {
  val maintainer = new Maintainer(this).start
  def act() {
    loop {
      react {
        case Open() =>
          val result = processor.process("threshold" :: "open" :: gateway.location :: gateway.path :: gateway.password :: Nil).waitFor
          logger.debug("Open " + gateway + " = " + result)
          Threshold.localPathMessage findFirstMatchIn (result.messages.reverse.flatten.mkString) map (_.group(1)) match {
            case Some(localPath) if result.success =>
              lurker ! WayFound(gateway, localPath)
            case _ =>
              lurker ! WayLost(gateway)
          }
        case Close() =>
          logger.debug("Close " + gateway)
          maintainer ! Deactivate()
          val result = processor.process("threshold" :: "close" :: gateway.location :: gateway.path :: Nil).waitFor
          lurker ! WayLost(gateway)
        case Maintain() => 
          logger.debug("Maintain " + gateway)
          maintainer ! Activate()
        case Destroy =>
          logger.debug("Destroy " + gateway)
          maintainer ! Destroy
          exit
        case unknown =>
          logger.info("" + unknown)
      }
    }
  }
}

case class Activate()
case class Deactivate()
case class Pulse()

class Maintainer(threshold: Threshold) extends Actor {
  val maintainDelay: Long = 5 * 60 * 1000L // five minutes (need only be twice the frequency of threshold opening)
  var active = false
  def act() {
    loop {
      react {
        case Activate() => 
          if (!active) {
            active = true
            self ! Pulse()
          }
        case Deactivate() =>
          active = false
        case Pulse() =>
          if (active) {
            val maintainer = self
            ActorPing.schedule(() => maintainer ! Pulse(), maintainDelay)
            threshold ! Open()
          }
        case Destroy =>
          exit
        case _ => 
      }
    }
  }
}

