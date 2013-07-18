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
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import util.{DatePresentation, Permission}
import model.{PresenceState, Environment, Gateway}
import concurrent.Future
import util.Context.playDefault
import gate.{T, Lock, Unlock, ScourAsap}
import play.api.Logger
import state.ChangedGateway

object Gateways extends Controller with Permission {

  lazy val stream = Environment.actorSystem.actorFor("/user/StateStream")
  lazy val watcher = Environment.actorSystem.actorFor("/user/Watcher")

  def list = PermittedAction { request =>
    val futureGs = Future( Gateway.forCultist(request.cultistId) )(util.Context.dbOperations)

    import util.Context.playDefault
    Async {
      futureGs.map(gs => Ok(Json.toJson(gs.map(_.toJson))))
    }
  }

  def add = PermittedAction(parse.json) { request =>
    val json: JsValue = request.body
    (json \ "uri", json \ "path", json \ "password", json \ "mode" \ "source", json \ "mode" \ "sink") match {
      case (JsString(uri), JsString(path), JsString(password), JsBoolean(source), JsBoolean(sink)) => {
        val gateway = new Gateway
        gateway.cultistId = request.cultistId
        gateway.location = uri.trim
        gateway.path = path.trim
        gateway.password = password.trim
        gateway.source = source
        gateway.sink = sink
        Gateway.save(gateway)
        stream ! ChangedGateway(gateway.id, request.cultistId)
        Ok("Ok")
      }
      case _ => {
        BadRequest
      }
    }
  }

  def get(id: Long) = PermittedAction { request =>
    val fGateway = Future( Gateway.find(id) )(util.Context.dbOperations)

    import util.Context.playDefault
    Async {
      fGateway.map{
        case Some(g) if (g.cultistId == request.cultistId) => Ok(g.toJson)
        case _ => BadRequest
      }
    }
  }

  def update(id: Long) = PermittedAction(parse.json) { request =>
    val json: JsValue = request.body
    (Gateway.find(id), json \ "uri", json \ "path", json \ "password", json \ "mode" \ "source", json \ "mode" \ "sink") match {
      case (Some(g), JsString(uri), JsString(path), passwordJs, JsBoolean(source), JsBoolean(sink)) if (g.cultistId == request.cultistId) => {
        g.location = uri.trim
        g.path = path.trim
        g.source = source
        g.sink = sink
        passwordJs match {
          case JsString(password) if (password.trim.length > 0) => g.password = password.trim
          case _ =>
        }
        Gateway.save(g)
        stream ! ChangedGateway(g.id, request.cultistId)
        Ok("Ok")
      }
      case _ => BadRequest
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

  def sources = InsaneAction { request =>
    val report = Gateway.sourceReport
    val at = T.now.getTime

    import util.Context.playDefault
    Async {
      report.map(lines =>
        Ok(views.html.report.sources(at, lines))
      )
    }

  }
}