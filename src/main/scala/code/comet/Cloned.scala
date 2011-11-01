package code.comet

import net.liftweb._
import http._
import util._
import code.model._
import scala.xml._

class Cloned extends CometActor with CometListener with ArtifactBinding {
  val factory = new ClonedSnapshotFactory
  var snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))

  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactCreated(a) =>
      // todo partialUpdate for all of this...
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactUpdated(a) =>
      // todo partialUpdate for all of this...
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactCloned(a) =>
      // todo partialUpdate for all of this...
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
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
