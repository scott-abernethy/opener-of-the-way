package controllers

import play.api._
import libs.json.JsValue
import play.api.mvc._
import util.{Context, ConcurrentUtil, Permission}
import state.StateStream
import concurrent.Future

object Application extends Controller with Permission {
  
  def index = PermittedAction { request =>
    Ok(views.html.index())
  }

  def stream() = {
    import Context.playDefault
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

  def modeTest = NonProductionAction( request =>
    Ok("Current mode is " + Play.mode(Play.current).toString)
  )

}