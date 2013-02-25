package controllers

import play.api.mvc.{Action, Controller}
import model.{PresenceState, Clone, ClonedSnapshotFactory, AwaitingSnapshotFactory}
import play.api.libs.json.Json
import util.{DatePresentation, Permission}

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

  def report = PermittedAction { request =>
    // TODO admin only
    // TODO async

    val report = Clone.report

    Ok(Json.toJson(report.map(line =>
      Json.obj(
        "for" -> line._4,
        "what" -> line._3,
        "requested" -> DatePresentation.atAbbreviation(line._1.requested.getTime),
        "presence" -> line._2.exists(_.state == PresenceState.present),
        "attempts" -> line._1.attempts,
        "attempted" -> DatePresentation.atAbbreviation(line._1.attempted.getTime)
      )
    )))
  }
}
