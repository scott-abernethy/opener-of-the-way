package code.comet

import net.liftweb._
import http._
import util._
import code.model._
import scala.xml._
import code.state.{ArtifactPack, ArtifactTouched, ArtifactServer}

class Cloned extends CometActor with CometListener with ArtifactBinding {
  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1)
  val factory = new ClonedSnapshotFactory
  var snapshot = factory.create(cultistId)

  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactPack(_, a, _, _, _) =>
      // todo partialUpdate for all of this...
      snapshot = factory.create(cultistId)
      reRender
    case _ =>
  }

  def render = {
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
      bindItem(in, i._1, i._2, None)
    )
  }
}
