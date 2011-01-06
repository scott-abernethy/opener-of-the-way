package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex

case class Open()
case class Close()
case class Maintain()
case object Destroy

object Threshold {
  val localPathMessage = new Regex(""">>>[\w ]+'([^']+)'""")
}

class Threshold(gateway: Gateway, lurker: Actor, processor: Processor) extends Actor {
  val maintainer = new Maintainer(this).start
  def act() {
    while (true) {
      receive {
        case Open() => 
          val (success, messages) = processor.waitFor("threshold" :: "open" :: gateway.location.is :: gateway.path.is :: gateway.password.is :: Nil)
          Threshold.localPathMessage findFirstMatchIn (messages.reverse.flatten.mkString) map (_.group(1)) match {
            case Some(localPath) if success =>
              lurker ! WayFound(gateway, localPath)
            case _ =>
              lurker ! WayLost(gateway)
          }
        case Close() =>
          maintainer ! Deactivate()
          val (success, _) = processor.waitFor("threshold" :: "close" :: gateway.location.is :: gateway.path.is :: Nil)
          lurker ! WayLost(gateway)
        case Maintain() => 
          maintainer ! Activate()
        case Destroy =>
          maintainer ! Destroy
          exit
        case _ =>
      }
    }
  }
}

case class Activate()
case class Deactivate()
case class Pulse()

class Maintainer(threshold: Threshold) extends Actor {
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
            Thread.sleep(60000)
            self ! Pulse()
          } 
        case Destroy =>
          exit
        case _ => 
      }
    }
  }
}

