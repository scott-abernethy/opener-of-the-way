package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._
import code.util.{Maintain, Maintainer}
import akka.actor.{ActorRef, Actor}

case class OpenGateway(gateway: Gateway)
case class CloseGateway(gateway: Gateway)

object Threshold {
  val localPathMessage = new Regex("""Gate opened[^']+'([^']+)'""")
}

class Threshold(processor: Processor) extends Actor with Loggable {

  def receive = {

    case OpenGateway(g) => {
      val result = processor.process("threshold" :: "open" :: g.location :: g.path :: g.password :: Nil).waitFor
      logger.debug("Open " + g + " = " + result)
      Threshold.localPathMessage findFirstMatchIn (result.messages.reverse.flatten.mkString) map (_.group(1)) match {
        case Some(localPath) if result.success =>
          self.reply( WayFound(g, localPath) )
        case _ =>
          self.reply( WayLost(g) )
      }
    }

    case CloseGateway(g) => {
      logger.debug("Close " + g)
      val result = processor.process("threshold" :: "close" :: g.location :: g.path :: Nil).waitFor
      self.reply(WayLost(g))
    }

  }
}