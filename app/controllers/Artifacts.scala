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

package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import model._
import concurrent.Future
import _root_.util.{Context, Permission}
import state.{ArtifactUnawaiting, ArtifactTouched, ArtifactAwaiting}
import gate.{T, AddArtifact, Summon}
import state.ArtifactUnawaiting
import gate.AddArtifact
import play.api.libs.json.JsBoolean
import scala.Some
import gate.Summon
import play.api.libs.json.JsNumber
import state.ArtifactAwaiting
import state.ArtifactTouched
import play.api.libs.json.JsObject

object Artifacts extends Controller with Permission {

  lazy val artifactServer = Environment.actorSystem.actorFor("/user/ArtifactServer")
  lazy val summoner = Environment.actorSystem.actorFor("/user/Summoner")
  lazy val keepers = Environment.actorSystem.actorFor("/user/Keepers")
  lazy val lurker = Environment.actorSystem.actorFor("/user/Lurker")

  def artifactWithStateJson(artifact: Artifact, state: Option[ArtifactState.Value], attempts: Option[Long], clones: Option[Int]): JsObject = {
    val cloneCount = clones.getOrElse(0)
    val attemptCount = attempts.getOrElse(0L).toInt
    artifact.toJson +
      ("state" -> ArtifactState.toJson(state)) +
      ("proffered" -> (state match {
      case Some(state) if ArtifactState.proffered_?(state) => JsBoolean(true)
      case _ => JsBoolean(false)
    })) +
      ("present" -> (state match {
      case Some(state) if ArtifactState.present_?(state) => JsBoolean(true)
      case _ => JsBoolean(false)
    })) +
      ("clones" -> JsNumber(cloneCount)) +
      ("attempts" -> JsNumber(attemptCount))
  }

  def add = NonProductionAction(parse.json) { request =>
    lurker ! AddArtifact(1, request.body \ "path" toString, 1234L, T.now)
    Ok("Added")
  }

  def log(count: Int, last: Long) = PermittedAction { request =>
    val lastOption = if (last == -1) None else Some(last)
    val snapshotFuture = ArtifactCloneSnapshot(request.cultistId, lastOption, count)
    import Context.playDefault

    val future: Future[Seq[JsObject]] = snapshotFuture.map { snapshot =>
      for (group <- snapshot.items.toSeq.reverse)
      yield Json.obj(
        "name" -> group._1,
        "items" -> group._2.map(a => artifactWithStateJson(a, snapshot.stateFor(a.id), None, snapshot.clonesFor(a.id)))
      )
    }

    Async {
      future.map(as => Ok(Json.toJson(as)))
    }
  }

  def list(q: String) = PermittedAction { request =>
    val items = new ArtifactCloneSearchFactory().create(request.cultistId, q)
    Ok(Json.toJson(items.map(x => artifactWithStateJson(x._1, x._2, None, x._3))))
  }

  def touch(id: Long) = PermittedAction { request =>
    val future = Future {
      Artifact.find(id).flatMap(_ toggleClone request.cultistId).map{
        case true => {
          artifactServer ! ArtifactTouched(ArtifactAwaiting(request.cultistId), id)
          summoner ! Summon(id)
          "Awaiting"
        }
        case false => {
          artifactServer ! ArtifactTouched(ArtifactUnawaiting(request.cultistId), id)
          "Unawaiting"
        }
      }
    }(Context.dbOperations)

    import Context.playDefault
    Async {
      future.map{
        case Some(res) => Ok(res)
        case _ => BadRequest("Failed")
      }
    }
  }

}
