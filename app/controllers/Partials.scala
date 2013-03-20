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

import play.api.mvc._

object Partials extends Controller {

  def home() = Action { request =>
    Ok(views.html.home())
  }

  def tome() = Action { request =>
    Ok(views.html.tome())
  }

  def addGateway() = Action { request =>
    Ok(views.html.addGateway("Add Gateway"))
  }

  def editGateway() = Action { request =>
    Ok(views.html.addGateway("Edit Gateway"))
  }

}
