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
import java.text.{SimpleDateFormat}
import java.util.{Date, TimeZone}
import collection.SortedMap
import collection.immutable.TreeMap

case object Subscribed

case class ArtifactCreated(artifact: Artifact)
case class ArtifactUpdated(artifactId: Long)
case class ArtifactCloned(artifactId: Long)

object ArtifactServer extends LiftActor with ListenerManager with Loggable {
  var createUpdate: AnyRef = "ignore"
  override def lowPriority = {
    case msg @ ArtifactCreated(a) =>
      logger.debug("Artifact created " + a)
      updateListeners(msg)
    case msg @ ArtifactUpdated(id) =>
      logger.debug("Artifact updated " + id)
      updateListeners(msg)
    case msg @ ArtifactCloned(id) =>
      logger.debug("Artifact cloned")
      updateListeners(msg)
    case _ => 
  }
}

class ArtifactLog extends CometActor with CometListener {
  val snapshot = new ArtifactCloneSnapshot
  snapshot.reload(Cultist.attending.is.map(_.id).getOrElse(-1))
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactCreated(a) =>
//      snapshot.add(a)
      // todo partialUpdate, though this rerender is good at the mo as it allows missing artifacts to be highlighted.
      snapshot.reload(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactUpdated(a) =>
      snapshot.update(a)
      partialUpdate(renderUpdate(a))
    case ArtifactCloned(a) =>
      snapshot.update(a)
      partialUpdate(renderUpdate(a))
    case _ =>
  }
  def render = {
    ClearClearable &
    ".log:group" #> bindGroups _
  }
  def bindGroups(in: NodeSeq): NodeSeq = {
    // use user timezone?
    snapshot.items.toSeq.reverse.flatMap((i: (String, List[Artifact])) => (
      ClearClearable &
      ".group:name *" #> i._1 &
      ".log:item" #> bindItems(i._2) _
    ) apply(in)).toSeq
  }
  def bindItems(artifacts: List[Artifact])(in: NodeSeq): NodeSeq = artifacts.flatMap(bindItem(in, _))
  def bindItem(in: NodeSeq, artifact: Artifact): NodeSeq = {
    val artifactState: Option[ArtifactState.Value] = snapshot.states.get(artifact.id)
    ClearClearable & 
    ".log:item [id]" #> idFor(artifact.id) &
    ".item:select *" #> selectOption(artifact, artifactState) &
    ".item:status *" #> artifactState.map(_.toString).getOrElse("?") &
    ".item:status [class+]" #> artifactState.flatMap(ArtifactState.style(_)).getOrElse("") &
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
//    for {
//      c <- Cultist.attending.is.toOption
//      a <- Artifact.find(id)
//    } yield a.clone(c)

    // todo return faster?
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.clone(c))) match {
      case Some(newStatus) =>
        ArtifactServer ! ArtifactCloned(id)
        JsCmds.Noop
      case _ =>
        JsCmds.Noop
    }
  }
  def itemDeselected(id: Long): JsCmd = {
    // todo return faster?
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.cancelClone(c))) match {
      case Some(newStatus) =>
        ArtifactServer ! ArtifactCloned(id)
        JsCmds.Noop
      case _ =>
        JsCmds.Noop
    }
  }
  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value]): NodeSeq = artifactState match {
    case Some(state) if (state == ArtifactState.glimpsed) =>
      selectCheckbox(artifact, false)
    case Some(state) if (state == ArtifactState.awaiting || state == ArtifactState.cloning || state == ArtifactState.lost) =>
      selectCheckbox(artifact, true)
    case _ => Unparsed("&nbsp;")
  }
  def selectCheckbox(artifact: Artifact, defaultSelected: Boolean): NodeSeq = {
    SHtml.ajaxCheckbox(defaultSelected, (s: Boolean) => if (s) itemSelected(artifact.id) else itemDeselected(artifact.id))
  }
  def idFor(id: Long): String = "artifact" + id
}
