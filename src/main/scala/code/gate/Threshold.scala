package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._
import akka.actor.{ActorRef, Actor}

case class OpenGateway(gateway: Gateway)
case class CloseGateway(gateway: Gateway)
case class OpenGateSuccess(gateway: Gateway, localPath: String)
case class OpenGateFailed(gateway: Gateway)
case class CloseGateSuccess(gateway: Gateway)
case class CloseGateFailed(gateway: Gateway)

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
          self.reply( OpenGateSuccess(g, localPath) )
        case _ =>
          self.reply( OpenGateFailed(g) )
      }
    }

    case CloseGateway(g) => {
      logger.debug("Close " + g)
      val result = processor.process("threshold" :: "close" :: g.location :: g.path :: Nil).waitFor
      if (result.success) {
        self.reply(CloseGateSuccess(g))
      } else {
        self.reply(CloseGateFailed(g))
      }
    }

  }
}