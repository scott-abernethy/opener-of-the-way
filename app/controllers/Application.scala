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

import play.api._
import libs.json.{Json, JsValue}
import play.api.mvc._
import util.{Context, ConcurrentUtil, Permission}
import state.StateStream
import concurrent.Future
import model.{Pseudonym, Cultist}
import views.html.defaultpages.badRequest
import play.api.libs.json.JsString
import model.Environment

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

  def disempower = Action(parse.json) { request =>
    import play.api.Play.current
    Play.configuration.getString("application.disempower-secret") match {
      case Some(secret) => {
        (request.body \ "auth") match {
          case JsString(auth) if (auth == secret) => {
            Environment.disempower()
            Ok("App Disempowered") 
          }
          case _ => {
            BadRequest("Auth does not match application.disempower-secret")
          }
        } 
      }
      case _ => {
        BadRequest("Shutdown API not enabled")
      }
    }
  }
}