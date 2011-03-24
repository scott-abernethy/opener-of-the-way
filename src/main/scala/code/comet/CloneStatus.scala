package code.comet

import net.liftweb._
import common._
import http._
import http.js._
import actor._
import util._
import Helpers._
import code.model._
import scala.xml._
import org.squeryl.PrimitiveTypeMode._

class CloneStatus extends CometActor with CometListener {
  def registerWith = ArtifactServer
  override def lowPriority = {
    case _ =>
      reRender
  }
  def render = {
    val clones = Cultist.attending.is.toOption match {
      case Some(cultist) => inTransaction( cultist.activeClones.toSeq )
      case _ => Nil
    }

    val state = clones.groupBy(_.state).values.toList.sortBy(_.head.state).map(cs => cs.size.toString + " " + cs.head.state.toString).join(", ")
    
    // ""
    // "4 queued"
    // "4 queued, 1 progressing"

    ClearClearable &
    ".clone:status" #> state
  }
}
