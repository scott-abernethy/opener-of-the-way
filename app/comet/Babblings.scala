//package comet
//
//import net.liftweb.util.ClearClearable
//import model.{GateState, Gateway, Cultist}
//import xml.{Node, NodeSeq}
//import net.liftweb.actor.LiftActor
//import net.liftweb.common.{Loggable, Full}
//import net.liftweb.http.ListenerManager._
//import net.liftweb.http._
//import js.JsCmds.{RedirectTo, SetValById}
//
//case class BabbleItem(source: Option[Cultist], text: String)
//
//class Babblings extends CometActor with CometListener
//{
//  var items: List[BabbleItem] = Nil
//  val initial = BabbleItem(None, "Service restarted")
//
//  def registerWith = BabblingsServer
//
//  override def lowPriority =
//  {
//    case init: List[BabbleItem] =>
//    {
//      items = init match {
//        case Nil => initial :: Nil
//        case list => list.take(8)
//      }
//      reRender
//    }
//    case update: BabbleItem =>
//    {
//      items = update :: items.filter(_ != initial).take(8)
//      reRender
//    }
//    case _ =>
//    {
//      reRender
//    }
//  }
//
//  def render =
//  {
////    Cultist.attending.is match {
////      case Full(cultist) =>
//        ClearClearable &
//        ".item" #> items.map{ x =>
//          val sign = x.source.map(_.sign).filter(x => x != null && x.size > 0).getOrElse("???")
//          val sigal = sign.charAt(0)
//          ".part *" #> <span>{ Cultist.sigalFor(x.source) } { x.text }</span>
//        }
////      case _ =>
////        ClearClearable
////    }
//  }
//}
//
//object BabblingsServer extends LiftActor with ListenerManager with Loggable
//{
//  var items: List[BabbleItem] = Nil
//
//  protected def createUpdate = items
//
//  override def lowPriority =
//  {
//    case babble: BabbleItem if (babble.text.trim.size > 0) =>
//    {
//      items = babble :: items.take(8)
//      updateListeners(babble)
//      logger.info(Cultist.signFor(babble.source) + "(" + babble.source.map(_.id).getOrElse(-1)  + ") babbled \"" + babble.text + "\"")
//    }
//  }
//}