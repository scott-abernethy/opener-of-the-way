package controllers

import play.api.mvc.{Controller}
import play.api.libs.json.{JsObject, Json}
import model._
import concurrent.Future
import util.{Permission}
import scala.concurrent.ExecutionContext.Implicits.global
import state.{ArtifactUnawaiting, ArtifactTouched, ArtifactAwaiting}
import gate.Summon

object Artifacts extends Controller with Permission {

  lazy val artifactServer = Environment.actorSystem.actorFor("/user/ArtifactServer")
  lazy val summoner = Environment.actorSystem.actorFor("/user/Summoner")
  lazy val keepers = Environment.actorSystem.actorFor("/user/Keepers")

  def artifactWithStateJson(artifact: Artifact, state: Option[ArtifactState.Value]): JsObject = {
    artifact.toJson + ("state" -> ArtifactState.toJson(state))
  }

  def log = PermittedAction { request =>
    val future = Future {
      val snapshot = new ArtifactCloneSnapshot(Artifact.notNewsAfter)
      snapshot.reload(request.cultistId)
      for (group <- snapshot.items.toSeq.reverse)
      yield Json.obj(
        "name" -> group._1,
        "items" -> group._2.map(a => artifactWithStateJson(a, snapshot.stateFor(a.id)))
      )
    }
    Async {
      future.map(as => Ok(Json.toJson(as)))
    }
  }

  def list(q: String) = PermittedAction { request =>
    val items = new ArtifactCloneSearchFactory().create(request.cultistId, q)
    Ok(Json.obj(
      "q" -> q,
      "size" -> items.size,
      "items" -> items.map(_._1.toJson)
    ))
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
    }
    Async {
      future.map{
        case Some(res) => Ok(res)
        case _ => BadRequest("Failed")
      }
    }
  }

}
