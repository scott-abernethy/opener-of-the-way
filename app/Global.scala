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

import db._
import play.api.{Play, Application, GlobalSettings, Logger}
import play.api.Play.current
import model.Environment

object Global extends GlobalSettings {

  var db: Option[Db] = None

  override def beforeStart(app: Application) {
    super.beforeStart(app)

    Logger.info("It is coming")
  }

  override def onStart(app: Application) {
    super.onStart(app)

    Logger.info("It is here")

    val data = if (Play.isTest) {
      new TestDb
    }
    else {
      new Db{}
    }
    data.init
    if (Play.isDev || Play.isTest) {
      data.clear
      data.populate
    }
    db = Some(data)

    Environment.start
  }

  override def onStop(app: Application) {
    super.onStop(app)

    Logger.info("It has gone")

    Environment.dispose

    db.foreach{_.close}
    db = None
  }
}
