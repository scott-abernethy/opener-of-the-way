package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsValue, JsObject, Json}
import model._
import concurrent.Future
import util.Permission
import scala.concurrent.ExecutionContext.Implicits.global
import state.{ArtifactTouched, ArtifactAwaiting}
import gate.Summon

object Artifacts extends Controller with Permission {

  lazy val artifactServer = Environment.actorSystem.actorFor("/user/ArtifactServer")
  lazy val summoner = Environment.actorSystem.actorFor("/user/Summoner")

  def log = Permitted { cultistId =>
    Action {
      val future = Future {
        val snapshot = new ArtifactCloneSnapshot(Artifact.notNewsAfter)
        snapshot.reload(cultistId)
        for (group <- snapshot.items.toSeq.reverse)
        yield Json.obj(
          "name" -> group._1,
          "items" -> group._2.map(a => a.toJson + ("state" -> ArtifactState.toJson(snapshot.stateFor(a.id))))
        )
      }
      Async {
        future.map(as => Ok(Json.toJson(as)))
      }
    }
  }

  def list(q: String) = Permitted { cultistId =>
    Action {
      val items = new ArtifactCloneSearchFactory().create(cultistId, q)
      Ok(Json.obj(
        "q" -> q,
        "size" -> items.size,
        "items" -> items.map(_._1.toJson)
      ))
    }
  }

  def touch(id: Long) = Permitted { cultistId =>
    Action {
      val future = Future {
        // TODO deselected case
        val res = Artifact.find(id).map(_ clone cultistId) getOrElse false
        if (res) {
          artifactServer ! ArtifactTouched(ArtifactAwaiting(cultistId), id)
          summoner ! Summon(id)
        }
        res
      }
      Async {
        future.map(res => if (res) Ok("Ok") else BadRequest("Failed"))
      }
    }
  }

//  def itemDeselected(id: Long) {
//    // todo return faster?
//    Cultist.attending.is.toOption.map(c => (c, Artifact.find(id).map(_.cancelClone(c)))) match {
//      case Some( (c, Some(newStatus)) ) =>
//        ArtifactServer ! ArtifactTouched(ArtifactUnawaiting(c.id), id)
//      case _ =>
//    }
//  }

  /*
  def stream() =
    WebSocket.using[String] { request =>

      // Log events to the console
      val in = Iteratee.foreach[String](println).mapDone { _ =>
        println("Disconnected")
      }

      // Send a single 'Hello!' message
      val out = Enumerator("Hello!")

      (in, out)
    }
    */

}
