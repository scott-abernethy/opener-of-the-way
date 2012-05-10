package code.comet

import net.liftweb._
import http._
import js.jquery.JqJsCmds
import js.JsCmds
import util._
import code.model._
import scala.xml._
import code.js.JquiJsCmds
import code.state._
import net.liftweb.util.Helpers._

class Cloned extends CometActor with CometListener with ArtifactBinding {
  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1)
  val factory = new ClonedSnapshotFactory
  var snapshot: ClonedSnapshot = null

  def registerWith = ArtifactServer

  override def lowPriority = {
    case pack @ ArtifactPack(ArtifactCloned(c), a, _, _, _) if (c == cultistId) => {
      // todo update the snapshot such that we don't have to reload on render
      val out: NodeSeq = (".cloned:item ^^" #> "not-used" andThen ".cloned:item" #> bindItems((a, pack.stateFor(cultistId)) :: Nil) _ andThen ".cloned:item [class+]" #> "hidden").apply(defaultHtml)
      partialUpdate(
        JqJsCmds.PrependHtml("cloneds", out ) & JquiJsCmds.BlindInFast(idFor(a.id))
      )
    }
    case pack @ ArtifactPack(ArtifactAwaiting(c), a, _, _, _) if (c == cultistId) => {
      // todo update the snapshot such that we don't have to reload on render
      partialUpdate(
        JquiJsCmds.BlindOutFast(idFor(a.id)) & JsCmds.After(1 second, JsCmds.Replace(idFor(a.id), NodeSeq.Empty))
      )
    }
    case _ =>
  }

  def render = {
    snapshot = factory.create(cultistId)
    ClearClearable &
    clonedPart()
  }

  def clonedPart(): CssSel = {
    if (snapshot.cloned.isEmpty) {
      ".cloned:item" #> NodeSeq.Empty
    } else {
      ".cloned-empty" #> NodeSeq.Empty &
      ".cloned:item" #> bindItems(snapshot.cloned) _
    }
  }

  def bindItems(xs: List[(Artifact, Option[ArtifactState.Value])])(in: NodeSeq): NodeSeq = {
    xs.flatMap( (i: (Artifact, Option[ArtifactState.Value])) =>
      bindItem(in, i._1, i._2, None, ".cloned:item [id]")
    )
  }

  override def idFor(id: Long) = "cloned" + id
}
