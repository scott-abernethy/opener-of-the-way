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

import play.api.mvc.Controller
import util.Permission
import play.api.Logger
import play.api.libs.json.{JsString, JsObject, Json}
import akka.actor.ActorRef
import model.{Pseudonym, Cultist, Environment}
import state.Babble
import akka.pattern.Patterns
import util.Context.playDefault

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
    import util.Context.playDefault
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
