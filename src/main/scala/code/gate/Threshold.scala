package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._

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
          val (success, messages) = processor.process("threshold" :: "open" :: gateway.location :: gateway.path :: gateway.password :: Nil).waitFor
          logger.debug("Open " + gateway + " = " + success + " " + messages)
          Threshold.localPathMessage findFirstMatchIn (messages.reverse.flatten.mkString) map (_.group(1)) match {
            case Some(localPath) if success =>
              lurker ! WayFound(gateway, localPath)
            case _ =>
              lurker ! WayLost(gateway)
          }
        case Close() =>
          logger.debug("Close " + gateway)
          maintainer ! Deactivate()
          val (success, _) = processor.process("threshold" :: "close" :: gateway.location :: gateway.path :: Nil).waitFor
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
  val interval = 10000L
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

