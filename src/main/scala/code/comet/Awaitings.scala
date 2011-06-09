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

class Awaitings extends CometActor with CometListener {
  val factory = new CloneSnapshotFactory
  var snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactCreated(a) =>
//      snapshot.add(a)
      // todo partialUpdate, though this rerender is good at the mo as it allows missing artifacts to be highlighted.
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactUpdated(a) =>
//      snapshot.update(a)
//      partialUpdate(renderUpdate(a))
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case ArtifactCloned(a) =>
//      snapshot.update(a)
//      partialUpdate(renderUpdate(a))
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case _ =>
  }
  def render = {
    ClearClearable &
    ".awaiting:item" #> bindAwaiting _ &
    ".cloned:item" #> bindCloned _
  }
  def bindAwaiting(in: NodeSeq): NodeSeq = {
    snapshot.awaiting.flatMap((i: (Artifact, Option[ArtifactState.Value])) => bindItem(in, i._1, i._2))
  }
  def bindCloned(in: NodeSeq): NodeSeq = {
    snapshot.cloned.flatMap((i: (Artifact, Option[ArtifactState.Value])) => bindItem(in, i._1, i._2))
  }
  def bindItem(in: NodeSeq, artifact: Artifact, artifactState: Option[ArtifactState.Value]): NodeSeq = {
    {ClearClearable &
    ".log:item [id]" #> idFor(artifact.id) &
    ".item:select *" #> selectOption(artifact, artifactState) &
    ".item:status *" #> artifactState.map(_.toString).getOrElse("?") &
    ".item:status [class+]" #> artifactState.flatMap(ArtifactState.style(_)).getOrElse("") &
    ".item:description *" #> artifact.description}.apply(in)
  }
//  def renderUpdate(id: Long): JsCmd = {
//    inTransaction(Artifact.find(id)) match {
//      case Some(updated) =>
//        JsCmds.Replace(idFor(id), bindItem((defaultXml \\ "li"), updated))
//      case _ =>
//        JsCmds.Noop
//    }
//  }
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
    case Some(state) if ArtifactState.possible_?(state) =>
      selectCheckbox(artifact, false)
    case Some(state) if ArtifactState.awaiting_?(state) =>
      selectCheckbox(artifact, true)
    case _ => Unparsed("&nbsp;")
  }
  def selectCheckbox(artifact: Artifact, defaultSelected: Boolean): NodeSeq = {
    SHtml.ajaxCheckbox(defaultSelected, (s: Boolean) => if (s) itemSelected(artifact.id) else itemDeselected(artifact.id))
  }
  def idFor(id: Long): String = "artifact" + id
}
