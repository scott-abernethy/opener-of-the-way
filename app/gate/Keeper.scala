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

package gate

import akka.actor.{Terminated, Props, ActorRef, Actor}
import model._
import play.Logger
import org.squeryl.PrimitiveTypeMode._
import Watcher._
import scala.Some
import akka.actor.Terminated
import gate.KeeperRouterApi.ToAll

object KeeperApi {
  case object Open
  case object StillOpen
  case object Closed
  case class Admit(xs: Seq[Presence])
  case class Release(xs: Seq[Clone])
  case class Cancel(xs: Seq[Clone])
}

/*
keeps track of in out for this 'node' .. being a unique gateway location
clones and presences
replaces maintainer? or does maintainer act as keeper router and factory?
start by just having one keeper.

>> or is there one of these per gateway, and another 'lock' thing that manages whether each keeper can progress.
 */

class Keeper(gatewayId: Long, locker: ActorRef, processs: Processs, watcher: ActorRef, artifactServer: ActorRef) extends Actor {
  import KeeperApi._

  var open: Boolean = false
//  var queuedPresences: Seq[Presence] = Nil
//  var queuedClones: Seq[Clone] = Nil

  def receive = {
    case Open => {
      open = true
      // scour (currently done in lurker)
      // check queue against db
      // process
      check()
    }
    case StillOpen => {
      if (open) {
        check()
      }
      else {
        Logger.warn("ODD STATE!")
        watcher ! GateFailed(gatewayId)
      }
    }
    case Closed => {
      open = false
      // stop processing
      for (child <- context.children) {
        child ! 'Cancel
      }
    }
    case Admit(xs) => {
//      queuedPresences = (queuedPresences ++ xs).distinct
      check()
    }
    case Release(xs) => {
//      queuedClones = (queuedClones ++ xs).distinct
      check()
    }
    case Cancel(xs) => {
      // TODO
//      queuedClones = queuedClones.filterNot(xs.toSet.contains)
//      for (child <- context.children; job <- xs) {
//        child ! CancelCloning(job)
//      }
    }
    case FinishedPresenting(presence, true) => {
//      queuedPresences = queuedPresences.filterNot(presence ==)
      check()
      context.parent ! ToAll(Release(Nil))
    }
    case FinishedPresenting(_, false) => {
      open = false
    }
    case FinishedCloning(clone, true) => {
//      queuedClones = queuedClones.filterNot(clone ==)
      check()
    }
    case FinishedCloning(_, false) => {
      open = false
    }
    case Terminated(ref) => {
      check()
    }
  }

  def check() {

    // TODO can do much better query here.

    def waitingPresences(): List[Presence] = {
      transaction(
        sourcesQuery()
          .toList
          .filter(x => x._1.state == PresenceState.called && x._2.id == gatewayId)
          .map(_._1)
      )
    }

    def waitingClones(): List[Clone] = {
      transaction(
        sinksQuery()
          .toList
          .filter(x => x._1.state == CloneState.awaiting && x._2.id == gatewayId && x._3.map(_.state) == Some(PresenceState.present))
          .map(_._1)
      )
    }

    // only do either presence or clone, not both (?) ... do presences first ... though perhaps that decision should be made by the locker?
    if (open && context.children.isEmpty) {
      start(waitingPresences().headOption orElse waitingClones().headOption)
    }
  }

  def start(item: AnyRef) {
    Logger.debug(self + " start " + item)
    item match {
      case Some(p: Presence) => {
        val presenter: ActorRef = context.actorOf(Props(new Presenter(processs, watcher, artifactServer)))
        context.watch(presenter)
        presenter ! StartPresenting(p)
      }
      case Some(c: Clone) => {
        val cloner: ActorRef = context.actorOf(Props(new Cloner(processs, watcher, artifactServer)))
        context.watch(cloner)
        cloner ! StartCloning(c)
      }
      case None => {
        // TODO Nothing to do, let the watcher know so it can close this?
      }
      case other => {
        Logger.debug("Can't start unexpected " + other)
      }
    }
  }
}