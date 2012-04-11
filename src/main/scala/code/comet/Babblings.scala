package code.comet

import net.liftweb.util.ClearClearable
import code.model.{GateState, Gateway, Cultist}
import xml.{Node, NodeSeq}
import net.liftweb.actor.LiftActor
import net.liftweb.common.{Loggable, Full}
import net.liftweb.http.ListenerManager._
import net.liftweb.http._
import js.JsCmds.{RedirectTo, SetValById}

case class BabbleItem(text: String)

class Babblings extends CometActor with CometListener
{
  var items: List[BabbleItem] = Nil
  val initial = BabbleItem("Yaji Ash-Shuthath!")

  def registerWith = BabblingsServer

  override def lowPriority =
  {
    case init: List[BabbleItem] =>
    {
      items = init match {
        case Nil => initial :: Nil
        case list => list
      }
      reRender
    }
    case update: BabbleItem =>
    {
      items = update :: items.filter(_ != initial).take(6)
      reRender
    }
    case _ =>
    {
      reRender
    }
  }

  def render =
  {
//    Cultist.attending.is match {
//      case Full(cultist) =>
        ClearClearable &
        ".item" #> items.map{ x =>
          ".part *" #> x.text
        }
//      case _ =>
//        ClearClearable
//    }
  }
}

object BabblingsServer extends LiftActor with ListenerManager with Loggable
{
  var items: List[BabbleItem] = Nil

  protected def createUpdate = items

  override def lowPriority =
  {
    case babble: BabbleItem if (babble.text.trim.size > 0) =>
    {
      items = babble :: items.take(6)
      updateListeners(babble)
    }
  }
}