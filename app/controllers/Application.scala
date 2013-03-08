package controllers

import play.api._
import libs.json.{Json, JsValue}
import play.api.mvc._
import util.{Context, ConcurrentUtil, Permission}
import state.StateStream
import concurrent.Future
import model.{Pseudonym, Cultist}

object Application extends Controller with Permission {
  
  def index = PermittedAction { request =>
    val pseudonym = Pseudonym.of(request.cultistId)
    Ok(views.html.index(pseudonym))
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

  def reports = InsaneAction { request =>
    Redirect(routes.Clones.load())
  }

}