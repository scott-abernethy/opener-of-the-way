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
//class Threshold(gateway: Gateway, lurker: ActorRef, processor: Processor) extends Actor with Loggable {
//  val maintainer = new Maintainer(this, 5 * 60 * 1000L).start // five minutes (need only be twice the frequency of threshold opening)

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

//  def act() {
//    loop {
//      react {
//        case Open() =>
//          val result = processor.process("threshold" :: "stage" :: gateway.location :: gateway.path :: gateway.password :: Nil).waitFor
//          logger.debug("Open " + gateway + " = " + result)
//          Threshold.localPathMessage findFirstMatchIn (result.messages.reverse.flatten.mkString) map (_.group(1)) match {
//            case Some(localPath) if result.success =>
//              lurker ! WayFound(gateway, localPath)
//            case _ =>
//              lurker ! WayLost(gateway)
//          }
//        case Close() =>
//          logger.debug("Close " + gateway)
//          maintainer ! Deactivate
//          val result = processor.process("threshold" :: "unstage" :: gateway.location :: gateway.path :: Nil).waitFor
//          lurker ! WayLost(gateway)
//        case Activate =>
//          logger.debug("Maintain " + gateway)
//          maintainer ! Activate
//        case Maintain =>
//          self ! Open()
//        case Destroy =>
//          logger.debug("Destroy " + gateway)
//          maintainer ! Destroy
//          exit
//        case unknown =>
//          logger.info("" + unknown)
//      }
//    }
//  }
}