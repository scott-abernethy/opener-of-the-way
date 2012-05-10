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
import code.state._

class ArtifactLog extends CometActor with CometListener with ArtifactBinding {

  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1L)

  val snapshot = new ArtifactCloneSnapshot
  snapshot.reload(cultistId)

  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactPack(ArtifactCreated, _, _, _, _) =>
      // todo partialUpdate, though this rerender is good at the mo as it allows missing artifacts to be highlighted.
      snapshot.reload(cultistId)
      reRender
    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
      partialUpdate(packUpdate((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item")), cultistId, pack))
    }
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
        JsCmds.Replace(idFor(id), bindItem((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item")), updated, snapshot.stateFor(updated.id), snapshot.clonesFor(updated.id)))
      case _ =>
        JsCmds.Noop
    }
  }
}
