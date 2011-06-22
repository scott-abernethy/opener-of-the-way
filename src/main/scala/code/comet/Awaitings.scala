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


class Awaitings extends CometActor with CometListener with ArtifactBinding {
  val factory = new CloneSnapshotFactory
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
    ".awaiting:item" #> bindItems(snapshot.awaiting) _ &
    ".cloned:item" #> bindItems(snapshot.cloned) _
  }
  def bindItems(xs: List[(Artifact, Option[ArtifactState.Value])])(in: NodeSeq): NodeSeq = {
    xs.flatMap((i: (Artifact, Option[ArtifactState.Value])) => bindItem(in, i._1, i._2))
  }
}
