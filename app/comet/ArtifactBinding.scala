/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//package comet
//
//import net.liftweb.util._
//import net.liftweb.util.Helpers._
//import net.liftweb.http.js.{JsCmds, JsCmd}
//import net.liftweb.http.SHtml
//import util.Size
//import state._
//import model._
//import gate.{T, Summon}
//import xml.{Text, Unparsed, NodeSeq}
//
//trait ArtifactBinding {
//
//  val zeroWidthSpace = "&#8203;"
//
//  def bindItem(in: NodeSeq, artifact: Artifact, artifactState: Option[ArtifactState.Value], clones: Option[Int], idSelector: String): NodeSeq = {
//    {
//      ClearClearable &
//      idSelector #> idFor(artifact.id) &
//      ".item:select" #> selectOption(artifact, artifactState, clones) &
//      ".item:status" #> artifactState.flatMap(ArtifactState.symbol(_)).getOrElse(ArtifactState.unknownSymbol) &
//      ".item:description" #> Unparsed(artifact.description.replaceAll("\\.", "." + zeroWidthSpace).replaceAll("_", "_" + zeroWidthSpace).replaceAll("/", "/" + zeroWidthSpace)) &
//      ".item-size" #> Size.short(artifact.length)
//    }.apply(in)
//  }
//
//  def itemSelected(id: Long): JsCmd = {
//    for {
//      c <- Cultist.attending.is.toOption
//      a <- Artifact.find(id)
//    } {
//      val result = a.clone(c)
//      if (result) {
//        ArtifactServer ! ArtifactTouched(ArtifactAwaiting(c.id), id)
//        Environment.summoner ! Summon(id)
//      }
//    }
//    // todo return faster?
//    JsCmds.Noop
//  }
//
//  def itemDeselected(id: Long): JsCmd = {
//    // todo return faster?
//    Cultist.attending.is.toOption.map(c => (c, Artifact.find(id).map(_.cancelClone(c)))) match {
//      case Some( (c, Some(newStatus)) ) =>
//        ArtifactServer ! ArtifactTouched(ArtifactUnawaiting(c.id), id)
//        JsCmds.Noop
//      case _ =>
//        JsCmds.Noop
//    }
//  }
//
//  def itemRefresh(id: Long): JsCmd = {
//    ArtifactServer ! ArtifactTouched(ArtifactRefresh(Cultist.attending.is.toOption.map(_.id)), id)
//    JsCmds.Noop
//  }
//
//  def selectOption(artifact: Artifact, artifactState: Option[ArtifactState.Value], clones: Option[Int]): NodeSeq = artifactState match {
//    case Some(ArtifactState.cloned) => {
//      SHtml.ajaxCheckbox(false, (s: Boolean) => JqConfirmationDialog("Again?", "Are you sure you want to clone this artifact AGAIN?", () => itemSelected(artifact.id), () => itemRefresh(artifact.id)))
//    }
//    case Some(state) if ArtifactState.possible_?(state) => {
//      SHtml.ajaxCheckbox(false, (s: Boolean) => itemSelected(artifact.id))
//    }
//    case Some(state) if ArtifactState.awaiting_?(state) => {
//      SHtml.ajaxCheckbox(true, (s: Boolean) => itemDeselected(artifact.id))
//    }
//    case Some(state) if ArtifactState.proffered_?(state) => {
//      <span class="clone-count">{ clones.map(_.toString + "x").getOrElse("0x") }</span>
//    }
//    case _ => {
//      Unparsed("&nbsp;")
//    }
//  }
//
//  def idFor(id: Long): String = "artifact" + id
//
//  def packUpdate(in: NodeSeq, cultistId: Long, pack: ArtifactPack, idSelector: String) = {
//    JsCmds.Replace(idFor(pack.artifact.id), bindItem(in, pack.artifact, pack.stateFor(cultistId), pack.cloneCount(), idSelector))
//  }
//}
