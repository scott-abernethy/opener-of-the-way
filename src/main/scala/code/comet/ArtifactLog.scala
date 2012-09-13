package code.comet

import net.liftweb._
import common._
import http._
import http.js._
import actor._
import js.jquery.JqJsCmds
import util._
import Helpers._
import code.model._
import scala.xml._
import org.squeryl.PrimitiveTypeMode._
import code.state._
import code.js.JquiJsCmds

class ArtifactLog extends CometActor with CometListener with ArtifactBinding {

  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1L)
  lazy val itemPart = (defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item"))

  val snapshot = new ArtifactCloneSnapshot(Artifact.notNewsAfter)
  var latestDayGroup: String = "2000-01-01"

  def registerWith = ArtifactServer

  override def lowPriority = {
    case pack @ ArtifactPack(ArtifactCreated, artifact, _, _, _) =>
      snapshot.discoveredGroup(artifact) match {
        case Some(day) if (day == latestDayGroup) => {
          val xml = bindItem(itemPart, artifact, pack.stateFor(cultistId), pack.cloneCount(), ".log:item [id]")
          // todo make helper for this hidden + blind in stuff
          val hidden = (".log:item [class+]" #> "hidden").apply(xml)
          partialUpdate(JqJsCmds.AppendHtml(snapshot.indexForGroup(latestDayGroup), hidden) & JquiJsCmds.BlindInFast(idFor(artifact.id)) )
        }
        case _ => {
          reRender
        }
      }
    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
      // todo update the snapshot such that we don't have to reload on render, or have snapshot available from ArtifactServer for fetch.
      partialUpdate(packUpdate(itemPart, cultistId, pack, ".log:item [id]"))
    }
    case _ => {}
  }

  def render = {
    snapshot.reload(cultistId)
    latestDayGroup = snapshot.latestDayGroup()
    ClearClearable &
    ".log:group" #> bindGroups _
  }

  def bindGroups(in: NodeSeq): NodeSeq = {
    // use user timezone?
    snapshot.items.toSeq.reverse.flatMap((i: (String, List[Artifact])) => (
      ClearClearable &
      ".log:group [id]" #> snapshot.indexForGroup(i._1) &
      ".group:name *" #> i._1 &
      ".log:item" #> bindItems(i._2) _
    ) apply(in)).toSeq
  }

  def bindItems(artifacts: List[Artifact])(in: NodeSeq): NodeSeq = {
    artifacts.flatMap{a =>
      bindItem(in, a, snapshot.stateFor(a.id), snapshot.clonesFor(a.id), ".log:item [id]")
    }
  }

}
