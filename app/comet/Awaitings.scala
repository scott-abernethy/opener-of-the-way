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
//import common.Loggable
//import net.liftweb.http._
//import js.jquery.JqJsCmds
//import js.JsCmds
//import net.liftweb.util._
//import scala.xml._
//import js.JquiJsCmds
//import util.Helpers._
//import state.{ArtifactPack, ArtifactServer, ArtifactTouched}
//import model.Clone._
//import gate.T
//import model._
//
//class Awaitings extends CometActor with CometListener with ArtifactBinding with Loggable {
//  val cultistId = Cultist.attending.is.map(_.id).getOrElse(-1L)
//  val factory = new AwaitingSnapshotFactory
//  var snapshot = factory.create(cultistId)
//
//  def registerWith = ArtifactServer
//
//  override def lowPriority = {
//    case pack @ ArtifactPack(_, a, _, _, _) => {
//      // todo shortcut, check for cultist is matching.
//      val s: Option[ArtifactState.Value] = pack.stateFor(cultistId)
//      val c: Option[Clone] = pack.cloneFor(cultistId)
//      val (snapshot2, action) = snapshot.update(a, s, c)
//        snapshot = snapshot2
//
//        val update = action match {
//          case Add(cid) => {
//            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _ andThen ".awaiting:item [class+]" #> "hidden").apply(defaultHtml)
//            partialUpdate( JqJsCmds.AppendHtml("awaitings", out ) & JquiJsCmds.BlindInFast(idOf(cid)) )
//          }
//          case Update(cid) => {
//            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _).apply(defaultHtml)
//            partialUpdate( JsCmds.Replace(idOf(cid), out ) )
//          }
//          case Remove(cid) => {
//            partialUpdate( JquiJsCmds.BlindOutFast(idOf(cid)) & JsCmds.After(1 second, JsCmds.Replace(idOf(cid), NodeSeq.Empty)) )
//          }
//          case Other => {
//            reRender()
//          }
//          case _ =>
//        }
//      }
//    case _ => {}
//  }
//
//  def idOf(cloneId: Long) = { "aw" + cloneId }
//
//  def render = {
//    ClearClearable &
//    ".awaiting:item" #> bindItems(snapshot.awaiting) _
//  }
//
//  def bindItems(xs: List[(Artifact, Option[ArtifactState.Value], Clone)])(in: NodeSeq): NodeSeq = {
//    xs.flatMap( i =>
//      (
//        ".awaiting:item [id]" #> idOf(i._3.id) &
//        awaitingMessage(i._3, i._2)
//      ).apply( bindItem(in, i._1, i._2, None, "not-used") )
//    )
//  }
//
//  def awaitingMessage(clone: Clone, state: Option[ArtifactState.Value]): CssSel = {
//    state match {
//      case Some(ArtifactState.awaitingLost) => {
//        ".item-message *" #> <span class="label label-important">{"Source artifact lost"}</span>
//      }
//      case Some(ArtifactState.awaiting) if (clone.requested.before(T.ago(Clone.poorWaitAfter))) => {
//        ".item-message *" #> <span class="label label-warning">{"Waiting on source artifact presence"}</span>
//      }
//      case Some(ArtifactState.awaitingPresent) if (clone.attempts > 2) => {
//        ".item-message *" #> <span class="label label-important">{"Attempted " + clone.attempts + "x without success. Check sink disk usage!"}</span>
//      }
//      case Some(ArtifactState.awaitingPresent) if (clone.attempts > 0) => {
//        ".item-message *" #> <span class="label label-warning">{"Attempted " + clone.attempts + "x without success. Check sink disk usage?"}</span>
//      }
//      case Some(ArtifactState.awaitingPresent) if (clone.requested.before(T.ago(Clone.poorWaitAfter))) => {
//      }
//      ".item-message *" #> <span class="label label-warning">{"Waiting on your sink gateway."}</span>
//      case _ => {
//        ".item-message" #> NodeSeq.Empty
//      }
//    }
//  }
//}
