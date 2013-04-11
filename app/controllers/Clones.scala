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
import model._
import play.api.libs.json.Json
import util.{Size, Shifting, DatePresentation, Permission}
import gate.T
import java.sql.Timestamp
import scala.concurrent.Future

object Clones extends Controller with Permission {

  def awaiting = PermittedAction { request =>
    val factory = new AwaitingSnapshotFactory
    val snapshot = factory.create(request.cultistId)
    Ok(Json.toJson(snapshot.awaiting.map(x => Artifacts.artifactWithStateJson(x._1, x._2, None))))
  }

  def history = PermittedAction { request =>
    // TODO do we need all these separate controllers for clone management? the view could deal with it?
    // could be one call to get all artifacts, and all clones.
    val factory = new ClonedSnapshotFactory
    val snapshot = factory.create(request.cultistId)
    Ok(Json.toJson(snapshot.cloned.map(x => Artifacts.artifactWithStateJson(x._1, x._2, None))))
  }

  def queue = InsaneAction { request =>
    val report = Clone.queue
    val at = T.now.getTime
    import util.Context.playDefault
    Async {
      report.map(lines =>
        Ok(views.html.report.queue(at, lines))
      )
    }
  }

  def load = InsaneAction { request =>
    val period: Timestamp = T.startOfSevenDayPeriod()
    val futureComplete = Clone.complete(period)
    val futureProffers = Artifact.proffers(period)
    val futureQueue = Clone.queueSummary()
    import util.Context.playDefault

    val futureReport: Future[List[(String, List[Artifact], List[(Clone, Artifact)])]] = for {
      complete <- futureComplete
      proffers <- futureProffers
      queue <- futureQueue
    }
    yield {
      val pmap = proffers.groupBy(i => DatePresentation.yearMonthDay(i.discovered.getTime))
      val cmap = complete.groupBy(i => DatePresentation.yearMonthDay(i._1.attempted.getTime))

      val history = for {
        day <- (pmap.keySet ++ cmap.keySet).toList.sorted.reverse
      }
      yield {
        (day, pmap.getOrElse(day, Nil), cmap.getOrElse(day, Nil))
      }

      ("Queued", Nil, queue) :: history
    }

    Async {
      futureReport.map(groups =>
        Ok(views.html.report.load(groups))
      )
    }
  }

}
