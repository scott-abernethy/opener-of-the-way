package controllers

import play.api.mvc.{Action, Controller}
import model.{ClonedSnapshotFactory, AwaitingSnapshotFactory}
import play.api.libs.json.Json
import util.Permission

object Clones extends Controller with Permission {

  def awaiting = Permitted { cultistId =>
    Action {
      val factory = new AwaitingSnapshotFactory
      val snapshot = factory.create(cultistId)
      val as = snapshot.awaiting.map(_._1)
      Ok(Json.toJson(as.map(_.toJson)))
    }
  }

  def history = Permitted { cultistId =>
    Action {
      // TODO do we need all these separate controllers for clone management? the view could deal with it?
      // could be one call to get all artifacts, and all clones.
      val factory = new ClonedSnapshotFactory
      val snapshot = factory.create(cultistId)
      val as = snapshot.cloned.map(_._1)
      Ok(Json.toJson(as.map(_.toJson)))
    }
  }
}
