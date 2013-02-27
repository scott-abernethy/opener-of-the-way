package controllers

import play.api.mvc.{Action, Controller}
import model.{PresenceState, Clone, ClonedSnapshotFactory, AwaitingSnapshotFactory}
import play.api.libs.json.Json
import util.{Shifting, DatePresentation, Permission}
import gate.T

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
        lines.groupBy(_._4)
      ).map( groups =>
        Ok(Json.toJson(groups.map(group =>
          Json.obj(
            "for" -> group._1,
            "awaiting" -> Json.toJson(group._2.map{line =>

              if ( line._2.exists(_.state == PresenceState.present) ) {
                Json.obj(
                  "what" -> line._3,
                  "requested" -> DatePresentation.ago(line._1.requested.getTime, at),
                  "cloneAttempts" -> line._1.attempts,
                  "cloneAttempted" -> DatePresentation.ago(line._1.attempted.getTime, at)
                )
              }
              else {
                Json.obj(
                  "what" -> line._3,
                  "requested" -> DatePresentation.ago(line._1.requested.getTime, at),
                  "presenceAttempts" -> line._2.map(_.attempts).getOrElse(0l).toLong,
                  "presenceAttempted" -> line._2.map(p => DatePresentation.ago(p.attempted.getTime, at)).getOrElse("-").toString
                )
              }
            })
          )
        )))
      )
    }
  }

  def load = InsaneAction { request =>
    val report = Clone.complete(T.startOfSevenDayPeriod())
    import util.Context.playDefault
    Async {
      report.map(clones =>
        clones.groupBy(clone => DatePresentation.yearMonthDay(clone.attempted.getTime))
      ).map(groups =>
        Ok(Json.toJson(groups.map(group =>
          Json.obj(
            "date" -> group._1,
            "cloned" -> group._2.size,
            "meanDuration" -> DatePresentation.duration(Shifting.calculateMedian(group._2))
          )
        )))
      )
    }
  }
}
