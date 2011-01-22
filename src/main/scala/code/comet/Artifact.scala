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

case class ArtifactCreated(artifact: Artifact)
case class ArtifactUpdated(artifactId: Long)

object ArtifactServer extends LiftActor with ListenerManager with Loggable {
  var createUpdate: AnyRef = "ignore" 
  override def lowPriority = {
    case msg @ ArtifactCreated(a) => 
      logger.debug("Artifact created " + a)
      createUpdate = msg
      updateListeners
    case msg @ ArtifactUpdated(id) =>
      logger.debug("Artifact updated " + id)
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
      if (!items.contains(a)) items = a :: items 
      reRender()
    case ArtifactUpdated(a) =>
      partialUpdate(renderUpdate(a))
    case _ =>
  }
  def render = 
    ClearClearable &
    ".log:item" #> bindItems _
  def bindItems(in: NodeSeq): NodeSeq = items.flatMap(bindItem(in, _))
  def bindItem(in: NodeSeq, artifact: Artifact): NodeSeq = {
    val artifactState: Option[ArtifactState.Value] = Cultist.attending.is.flatMap(artifact.stateFor(_))
    ClearClearable & 
    ".log:item [id]" #> idFor(artifact.id) &
    ".item:select *" #> selectOption(artifact, artifactState) &
    ".item:status *" #> artifactState.map(_.toString).getOrElse("?") &
    ".item:description *" #> artifact.description apply(in)
  }
  def renderUpdate(id: Long): JsCmd = {
    inTransaction(Artifact.find(id)) match {
      case Some(updated) =>
        JsCmds.Replace(idFor(id), bindItem((defaultXml \\ "li"), updated))
      case _ =>
        JsCmds.Noop
    }
  }
  def itemSelected(id: Long): JsCmd = {
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.clone(c))) match {
      case Some(newStatus) => renderUpdate(id)
      case _ => JsCmds.Noop
    }
  }
  def itemDeselected(id: Long): JsCmd = {
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.cancelClone(c))) match {
      case Some(newStatus) => JsCmds.Noop
      case _ => JsCmds.Noop
    }
  }
  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value]): NodeSeq = artifactState match {
    case Some(state) if (state == ArtifactState.available) =>
      selectCheckbox(artifact, false)
    case Some(state) if (state == ArtifactState.waiting || state == ArtifactState.progressing || state == ArtifactState.failed) =>
      selectCheckbox(artifact, true)
    case _ => Unparsed("&nbsp;")
  }
  def selectCheckbox(artifact: Artifact, defaultSelected: Boolean): NodeSeq = {
    SHtml.ajaxCheckbox(defaultSelected, (s: Boolean) => if (s) itemSelected(artifact.id) else itemDeselected(artifact.id))
  }
  def idFor(id: Long): String = "artifact" + id
}
