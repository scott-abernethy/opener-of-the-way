package controllers

import play.api._
import libs.json.JsValue
import play.api.mvc._
import util.{ConcurrentUtil, Permission}
import state.StateStream
import concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object Application extends Controller with Permission {
  
  def index = PermittedAction { request =>
    Ok(views.html.index())
  }

  def stream() =
    WebSocket.async[JsValue] {
      request =>
        getCultistId(request).map { cultistId =>
          Logger.debug("Stream socket request for " + cultistId)
          StateStream.follow(cultistId)
        }.getOrElse{
          Logger.warn("Unauthorized stream socket request!")
          Future(ConcurrentUtil.errorSocket("Unauthorized"))
        }
    }

}