package code.comet

import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.SHtml
import xml.{Unparsed, NodeSeq}
import code.model.{Environment, Cultist, ArtifactState, Artifact}
import code.gate.Summon
import code.util.Size
import code.js.JqConfirmationDialog

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
      ".item:description *" #> artifact.description &
      ".item-size *" #> Size.short(artifact.length)
    }.apply(in)
  }

  def itemSelected(id: Long): JsCmd = {
    val result = for {
      c <- Cultist.attending.is.toOption
      a <- Artifact.find(id)
    } yield a.clone(c)
    // todo return faster?
    result.filter(_ == true).foreach { i =>
      ArtifactServer ! ArtifactTouched(ArtifactAwaiting, id)
      Environment.summoner ! Summon(id)
    }
    JsCmds.Noop
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

  def itemRefresh(id: Long): JsCmd = {
    ArtifactServer ! ArtifactTouched(ArtifactUpdated, id)
    JsCmds.Noop
  }

  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value], clones: Option[Int]): NodeSeq = artifactState match {
    case Some(ArtifactState.cloned) => {
      SHtml.ajaxCheckbox(false, (s: Boolean) => JqConfirmationDialog("Again?", "Are you sure you want to clone this artifact AGAIN?", () => itemSelected(artifact.id), () => itemRefresh(artifact.id)))
    }
    case Some(state) if ArtifactState.possible_?(state) => {
      SHtml.ajaxCheckbox(false, (s: Boolean) => itemSelected(artifact.id))
    }
    case Some(state) if ArtifactState.awaiting_?(state) => {
      SHtml.ajaxCheckbox(true, (s: Boolean) => itemDeselected(artifact.id))
    }
    case Some(state) if ArtifactState.proffered_?(state) => {
      <span class="clone-count">{ clones.map(_.toString + "x").getOrElse("") }</span>
    }
    case _ => {
      Unparsed("&nbsp;")
    }
  }
  
  def idFor(id: Long): String = "artifact" + id
}
