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

object ArtifactServer extends LiftActor with ListenerManager with Loggable {
  var createUpdate: AnyRef = "ignore"
  override def lowPriority = {
    case msg @ ArtifactCreated(a) =>
      logger.debug("Artifact created " + a)
      updateListeners(msg)
    case msg @ ArtifactUpdated(id) =>
      logger.debug("Artifact updated " + id)
      updateListeners(msg)
    case _ => 
  }
}

class ArtifactLog extends CometActor with CometListener {
  val dateF = {
    val f = new SimpleDateFormat("yyyy-MM-dd',' EEEE")
    f.setTimeZone(TimeZone.getDefault)
    f
  }
  var items = reload
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactCreated(a) =>
      items = insertItem(items, a)
      reRender
    case ArtifactUpdated(a) =>
      partialUpdate(renderUpdate(a))
    case _ =>
  }
  def reload: TreeMap[String, List[Artifact]] = {
    var m = new TreeMap[String, List[Artifact]]
    Artifact.all.foreach(a => m = insertItem(m, a)) // already sorted
    m
  }
  def insertItem(into: TreeMap[String, List[Artifact]], a: Artifact): TreeMap[String, List[Artifact]] = {
    Option(a.discovered).map(timestamp => new Date(timestamp.getTime)).map(dateF format _) match {
      case Some(key) =>
        val as = into.getOrElse(key, Nil)
        if (!as.contains(a)) into + ((key, a :: as)) else into
      case other => into
    }
  }
  def render = 
    ClearClearable &
    ".log:group" #> bindGroups _
  def bindGroups(in: NodeSeq): NodeSeq = {
    // use user timezone?
    items.toSeq.reverse.flatMap((i: (String, List[Artifact])) => (
      ClearClearable &
      ".group:name *" #> i._1 &
      ".log:item" #> bindItems(i._2) _
    ) apply(in)).toSeq
  }
  def bindItems(artifacts: List[Artifact])(in: NodeSeq): NodeSeq = artifacts.flatMap(bindItem(in, _))
  def bindItem(in: NodeSeq, artifact: Artifact): NodeSeq = {
    val artifactState: Option[ArtifactState.Value] = Cultist.attending.is.flatMap(artifact.stateFor(_))
    ClearClearable & 
    ".log:item [id]" #> idFor(artifact.id) &
    ".item:select *" #> selectOption(artifact, artifactState) &
    ".item:status *" #> artifactState.map(_.toString).getOrElse("?") &
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
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.clone(c))) match {
      case Some(newStatus) => renderUpdate(id)
      case _ => JsCmds.Noop
    }
  }
  def itemDeselected(id: Long): JsCmd = {
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.cancelClone(c))) match {
      case Some(newStatus) => renderUpdate(id)
      case _ => JsCmds.Noop
    }
  }
  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value]): NodeSeq = artifactState match {
    case Some(state) if (state == ArtifactState.available) =>
      selectCheckbox(artifact, false)
    case Some(state) if (state == ArtifactState.waiting || state == ArtifactState.progressing || state == ArtifactState.failed) =>
      selectCheckbox(artifact, true)
    case _ => Unparsed("&nbsp;")
  }
  def selectCheckbox(artifact: Artifact, defaultSelected: Boolean): NodeSeq = {
    SHtml.ajaxCheckbox(defaultSelected, (s: Boolean) => if (s) itemSelected(artifact.id) else itemDeselected(artifact.id))
  }
  def idFor(id: Long): String = "artifact" + id
}
