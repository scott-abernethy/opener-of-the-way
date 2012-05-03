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

sealed class ArtifactChange
case object ArtifactCreated extends ArtifactChange
case class ArtifactRefresh(selectCultistId: Option[Long]) extends ArtifactChange
case object ArtifactAwaiting extends ArtifactChange
case object ArtifactUnawaiting extends ArtifactChange
case object ArtifactPresenting extends ArtifactChange
case object ArtifactPresented extends ArtifactChange
case object ArtifactPresentFailed extends ArtifactChange
case object ArtifactCloning extends ArtifactChange
case object ArtifactCloned extends ArtifactChange
case object ArtifactCloneFailed extends ArtifactChange
case class ArtifactTouched(change: ArtifactChange, artifactId: Long)

object ArtifactServer extends LiftActor with ListenerManager with Loggable {
  var createUpdate: AnyRef = "ignore"
  override def lowPriority = {
    case msg =>
      logger.info(msg.toString)
      updateListeners(msg)
  }
}

// TODO much of the work here is common across user sessions... scale

class ArtifactLog extends CometActor with CometListener with ArtifactBinding {
  val snapshot = new ArtifactCloneSnapshot
  snapshot.reload(Cultist.attending.is.map(_.id).getOrElse(-1))

  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactTouched(ArtifactCreated, _) =>
//      snapshot.add(a)
      // todo partialUpdate, though this rerender is good at the mo as it allows missing artifacts to be highlighted.
      snapshot.reload(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactTouched(_, a) =>
      snapshot.update(a)
      partialUpdate(renderUpdate(a))
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
        JsCmds.Replace(idFor(id), bindItem((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item")), updated, snapshot.stateFor(updated.id), snapshot.clonesFor(updated.id)))
      case _ =>
        JsCmds.Noop
    }
  }
}
