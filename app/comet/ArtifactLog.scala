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
//import net.liftweb._
//import common._
//import http._
//import http.js._
//import actor._
//import js.jquery.JqJsCmds
//import util._
//import Helpers._
//import model._
//import scala.xml._
//import org.squeryl.PrimitiveTypeMode._
//import state._
//import js.JquiJsCmds
//
//class ArtifactLog extends CometActor with CometListener with ArtifactBinding {
//
//  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1L)
//  lazy val itemPart = (defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item"))
//
//  val snapshot = new ArtifactCloneSnapshot(Artifact.notNewsAfter)
//  var latestDayGroup: String = "2000-01-01"
//
//  def registerWith = ArtifactServer
//
//  override def lowPriority = {
//    case pack @ ArtifactPack(ArtifactCreated, artifact, _, _, _) =>
//      snapshot.discoveredGroup(artifact) match {
//        case Some(day) if (day == latestDayGroup) => {
//          val xml = bindItem(itemPart, artifact, pack.stateFor(cultistId), pack.cloneCount(), ".log:item [id]")
//          // todo make helper for this hidden + blind in stuff
//          val hidden = (".log:item [class+]" #> "hidden").apply(xml)
//          partialUpdate(JqJsCmds.AppendHtml(snapshot.indexForGroup(latestDayGroup), hidden) & JquiJsCmds.BlindInFast(idFor(artifact.id)) )
//        }
//        case _ => {
//          reRender
//        }
//      }
//    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
//      // todo update the snapshot such that we don't have to reload on render, or have snapshot available from ArtifactServer for fetch.
//      partialUpdate(packUpdate(itemPart, cultistId, pack, ".log:item [id]"))
//    }
//    case _ => {}
//  }
//
//  def render = {
//    snapshot.reload(cultistId)
//    latestDayGroup = snapshot.latestDayGroup()
//    ClearClearable &
//    ".log:group" #> bindGroups _
//  }
//
//  def bindGroups(in: NodeSeq): NodeSeq = {
//    // use user timezone?
//    snapshot.items.toSeq.reverse.flatMap((i: (String, List[Artifact])) => (
//      ClearClearable &
//      ".log:group [id]" #> snapshot.indexForGroup(i._1) &
//      ".group:name *" #> i._1 &
//      ".log:item" #> bindItems(i._2) _
//    ) apply(in)).toSeq
//  }
//
//  def bindItems(artifacts: List[Artifact])(in: NodeSeq): NodeSeq = {
//    artifacts.flatMap{a =>
//      bindItem(in, a, snapshot.stateFor(a.id), snapshot.clonesFor(a.id), ".log:item [id]")
//    }
//  }
//
//}
