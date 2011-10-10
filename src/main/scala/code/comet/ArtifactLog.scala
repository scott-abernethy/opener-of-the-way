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
      logger.debug("Artifact clone requested")
      updateListeners(msg)
    case _ => 
  }
}

class ArtifactLog extends CometActor with CometListener with ArtifactBinding {
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

  def bindItems(artifacts: List[Artifact])(in: NodeSeq): NodeSeq = {
    artifacts.flatMap{a =>
      bindItem(in, a, snapshot.stateFor(a.id), snapshot.clonesFor(a.id))
    }
  }

  def renderUpdate(id: Long): JsCmd = {
    inTransaction(Artifact.find(id)) match {
      case Some(updated) =>
        JsCmds.Replace(idFor(id), bindItem((defaultHtml \\ "li"), updated, snapshot.stateFor(updated.id), snapshot.clonesFor(updated.id)))
      case _ =>
        JsCmds.Noop
    }
  }
}
