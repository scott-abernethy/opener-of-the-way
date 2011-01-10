package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex
import net.liftweb.common._

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
    while (true) {
      receive {
        case Open() => 
          val (success, messages) = processor.waitFor("threshold" :: "open" :: gateway.location.is :: gateway.path.is :: gateway.password.is :: Nil)
          logger.info("Open " + success + " " + messages)
          Threshold.localPathMessage findFirstMatchIn (messages.reverse.flatten.mkString) map (_.group(1)) match {
            case Some(localPath) if success =>
              lurker ! WayFound(gateway, localPath)
            case _ =>
              lurker ! WayLost(gateway)
          }
        case Close() =>
          logger.info("Close")
          maintainer ! Deactivate()
          val (success, _) = processor.waitFor("threshold" :: "close" :: gateway.location.is :: gateway.path.is :: Nil)
          lurker ! WayLost(gateway)
        case Maintain() => 
          logger.info("Maintain")
          maintainer ! Activate()
        case Destroy =>
          logger.info("Destroy")
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
  val interval = 10000L
  var active = false
  def act() {
    while (true) {
      receive {
        case Activate() => 
          if (!active) {
            active = true 
            self ! Pulse()
          }
        case Deactivate() => 
          active = false
        case Pulse() => 
          if (active) { 
            threshold ! Open()
            Thread.sleep(interval)
            self ! Pulse()
          } 
        case Destroy =>
          exit
        case _ => 
      }
    }
  }
}

