package controllers

import play.api.mvc.Controller
import util.Permission
import play.api.Logger
import play.api.libs.json.{JsString, JsObject, Json}
import akka.actor.ActorRef
import model.{Pseudonym, Cultist, Environment}
import state.Babble
import akka.pattern.Patterns
import scala.concurrent.ExecutionContext.Implicits.global

object Babbling extends Controller with Permission {

  lazy val server: ActorRef = Environment.actorSystem.actorFor("/user/BabbleServer")

  def add = PermittedAction(parse.json) { request =>
    request.body \ "text" match {
      case JsString(text) if (text.trim.size > 0) => {
        Logger.debug("Babble add for " + request.cultistId + " of " + text)
        val signFor = Pseudonym.of(request.cultistId)
        server ! Babble(signFor, text)
        Ok("Added")
      }
      case _ => {
        BadRequest
      }
    }
  }

  def list = PermittedAction { request =>
    val future = Patterns.ask(server, 'List, 10000L)
    Async {
      future.map {
        case Nil => Ok(Json.toJson(List.empty[JsObject]))
        case b :: bs => Ok(Json.toJson((b :: bs).collect{case Babble(who, text) => Json.obj("who" -> who, "text" -> text)}))
        case _ => BadRequest
      }
    }
  }

}
