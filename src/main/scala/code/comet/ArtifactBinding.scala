package code.comet

import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.SHtml
import xml.{Unparsed, NodeSeq}
import code.model.{Environment, Cultist, ArtifactState, Artifact}
import code.gate.Summon

trait ArtifactBinding {

  def bindItems(items: Seq[(Artifact, Option[ArtifactState.Value])])(in: NodeSeq): NodeSeq = {
    items.flatMap(i => bindItem(in, i._1, i._2, None))
  }

  def bindItem(in: NodeSeq, artifact: Artifact, artifactState: Option[ArtifactState.Value], clones: Option[Int]): NodeSeq = {
    {
      ClearClearable &
      ".log:item [id]" #> idFor(artifact.id) &
      ".item:select *" #> selectOption(artifact, artifactState, clones) &
      ".item:status *" #> artifactState.flatMap(ArtifactState.symbol(_)).getOrElse(ArtifactState.unknownSymbol) &
      ".item:description *" #> artifact.description
    }.apply(in)
  }

  def itemSelected(id: Long): JsCmd = {
//    for {
//      c <- Cultist.attending.is.toOption
//      a <- Artifact.find(id)
//    } yield a.clone(c)
    // todo return faster?
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.clone(c))) match {
      case Some(newStatus) =>
        ArtifactServer ! ArtifactTouched(ArtifactAwaiting, id)
        Environment.summoner ! Summon(id)
        JsCmds.Noop
      case _ =>
        JsCmds.Noop
    }
  }

  def itemDeselected(id: Long): JsCmd = {
    // todo return faster?
    Cultist.attending.is.toOption.flatMap(c => Artifact.find(id).map(_.cancelClone(c))) match {
      case Some(newStatus) =>
        ArtifactServer ! ArtifactTouched(ArtifactUnawaiting, id)
        JsCmds.Noop
      case _ =>
        JsCmds.Noop
    }
  }

  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value], clones: Option[Int]): NodeSeq = artifactState match {
    case Some(state) if ArtifactState.possible_?(state) =>
      selectCheckbox(artifact, false)
    case Some(state) if ArtifactState.awaiting_?(state) =>
      selectCheckbox(artifact, true)
    case Some(state) if ArtifactState.proffered_?(state) =>
      <span class="clone-count">{ clones.map(_ + "x").getOrElse("") }</span>
    case _ => Unparsed("&nbsp;")
  }
  
  def selectCheckbox(artifact: Artifact, defaultSelected: Boolean): NodeSeq = {
    SHtml.ajaxCheckbox(defaultSelected, (s: Boolean) => if (s) itemSelected(artifact.id) else itemDeselected(artifact.id))
  }

  def idFor(id: Long): String = "artifact" + id
}
