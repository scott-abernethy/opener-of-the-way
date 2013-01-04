package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import util.Permission
import model.Gateway
import concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Gateways extends Controller with Permission {

  def list = PermittedAction { request =>
    val futureGs = Future( Gateway.forCultist(request.cultistId) )
    Async {
      futureGs.map(gs => Ok(Json.toJson(gs.map(_.toJson))))
    }
  }
}