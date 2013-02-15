package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import util.Permission
import model.{Environment, Gateway}
import concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import comet.ChangedGateway
import gate.{Lock,Unlock,ScourAsap}

object Gateways extends Controller with Permission {

  lazy val stream = Environment.actorSystem.actorFor("/user/StateStream")
  lazy val watcher = Environment.actorSystem.actorFor("/user/Watcher")

  def list = PermittedAction { request =>
    val futureGs = Future( Gateway.forCultist(request.cultistId) )
    Async {
      futureGs.map(gs => Ok(Json.toJson(gs.map(_.toJson))))
    }
  }

  def add = PermittedAction(parse.json) { request =>
    val json: JsValue = request.body
    (json \ "uri", json \ "path", json \ "password", json \ "mode" \ "source", json \ "mode" \ "sink") match {
      case (JsString(uri), JsString(path), JsString(password), JsBoolean(source), JsBoolean(sink)) => {
        val gateway = new Gateway
        gateway.cultistId = 1L//request.cultistId
        gateway.location = uri.trim
        gateway.path = path.trim
        gateway.password = password.trim
        //val (source, sink) = Gateway.encodeModeMap.get(mode.trim).getOrElse((false, false))
        gateway.source = source
        gateway.sink = sink
        Gateway.save(gateway)
        stream ! ChangedGateway(gateway.id, 1L/*request.cultistId*/)
        Ok("Ok")
      }
      case _ => {
        BadRequest
      }
    }
  }

  def lock = PermittedAction(parse.json) { request =>
    val msg = request.body \ "enable" match {
      case JsBoolean(false) => Unlock(request.cultistId)
      case _ => Lock(request.cultistId)
    }
    watcher ! msg
    Ok("Ok")
  }

  def scour = PermittedAction { request =>
    watcher ! ScourAsap(request.cultistId)
    Ok("Ok")
  }
}