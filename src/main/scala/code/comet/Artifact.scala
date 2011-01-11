package code.comet

import net.liftweb._
import common._
import http._
import actor._
import util._
import Helpers._
import code.model._
import scala.xml._

case class ArtifactCreated(artifact: Artifact)

object ArtifactServer extends LiftActor with ListenerManager with Loggable {
  var createUpdate: AnyRef = "ignore" 
  override def lowPriority = {
    case msg @ ArtifactCreated(a) => 
      logger.info("Artifact created " + a)
      createUpdate = msg
      updateListeners
    case _ => 
  }
}

class ArtifactLog extends CometActor with CometListener {
  var items: List[Artifact] = Artifact.all
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactCreated(a) =>
      items = a :: items
      reRender()
    case _ => 
      reRender()
  }
  def render = 
    ClearClearable &
    ".log:item" #> bindItems _
  def bindItems(in: NodeSeq): NodeSeq = items.flatMap(bindItem(in, _))
  def bindItem(in: NodeSeq, artifact: Artifact): NodeSeq =
    ClearClearable & 
    ".item:select" #> "" &
    ".item:status" #> Cultist.attending.is.flatMap(artifact.stateFor(_)).map(_.toString).getOrElse("?") & 
    ".item:description" #> artifact.description apply(in)
}
