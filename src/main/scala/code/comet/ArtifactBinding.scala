package code.comet

import net.liftweb.util._
import net.liftweb.util.Helpers._
import code.model.{Cultist, ArtifactState, Artifact}
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.SHtml
import xml.{Unparsed, NodeSeq}

trait ArtifactBinding {
  def bindItems(items: Seq[(Artifact, Option[ArtifactState.Value])])(in: NodeSeq): NodeSeq = {
    items.flatMap(i => bindItem(in, i._1, i._2))
  }
  def bindItem(in: NodeSeq, artifact: Artifact, artifactState: Option[ArtifactState.Value]): NodeSeq = {
    {ClearClearable &
    ".log:item [id]" #> idFor(artifact.id) &
    ".item:select *" #> selectOption(artifact, artifactState) &
    ".item:status *" #> artifactState.map(_.toString).getOrElse("?") &
    ".item:status [class+]" #> artifactState.flatMap(ArtifactState.style(_)).getOrElse("") &
    ".item:description *" #> artifact.description}.apply(in)
  }
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
