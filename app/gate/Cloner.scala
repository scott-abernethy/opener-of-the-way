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

import model._
import model.Mythos._
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import state._
import akka.actor.{ActorRef, Actor}
import play.api.Logger

// TODO merge with Presenter

case class StartCloning(job: Clone)
case class FinishedCloning(job: Clone, success: Boolean)
case class CancelCloning(job: Clone)

class Cloner(val processs: Processs, val watcher: ActorRef, val artifactServer: ActorRef) extends Actor {
  var cur: Option[Clone] = None
  var requester: ActorRef = context.system.deadLetters
  var destroyHandle: Destroyable = new NonDestroyable

  def receive = {
    case StartCloning(job) if (cur.isEmpty) => {
      requester = sender
      start(job)
    }
    case CancelCloning(job) if (cur.exists(_ == job)) => {
      cancel
    }
    case exit: Exit => {
      cur.foreach(attempted(_, exit))
    }
    case 'Cancel => {
      cancel
    }
    case msg => {
      unhandled(msg)
    }
  }

  def start(job: Clone) {
    Logger.debug(this + " start " + job)
    cur = Some(job)
    job.state = CloneState.cloning
    job.attempted = T.now
    transaction { clones.update(job) }
    artifactServer ! ArtifactTouched(ArtifactCloning(job.forCultistId), job.artifactId)
    transaction {
      // todo fix with better comprehension
      for {
        destFileName <- job.artifact.map(_.fileName)
        dest <- job.forCultist.flatMap(_.destination).map(_.clonesPath).map(new File(_, destFileName).getPath)
        src = job.artifactId.toString
      } yield ("cloner" :: escapeString(src) :: escapeString(dest) ::  Nil)
    } match {
      case Some(command) => {
        val (future, destroyable) = processs.start(command)
        destroyHandle = destroyable
        import util.Context.defaultOperations
        future.onFailure {
          case e: Exception => self ! Exit(-1, e.getMessage :: Nil, -1)
        }
        future.onSuccess {
          case exit: Exit => self ! exit
        }
      }
      case None => {
        failedAttempt(job)
      }
    }
  }

  def cancel {
    destroyHandle.destroy
    destroyHandle = new NonDestroyable
    cur.foreach(failedAttempt _)
  }

  def attempted(c: Clone, result: Exit) {
    Logger.debug("Cloner result " + result)
    val success = result.exitValue == 0
    c.state = if (success) CloneState.cloned else CloneState.awaiting
    c.attempts = c.attempts + 1
    c.duration = result.duration
    transaction { clones.update(c) }
    cur = None
    if (!success) watcher ! CloneFailed(c)
    artifactServer ! ArtifactTouched(if (success) ArtifactCloned(c.forCultistId) else ArtifactCloneFailed(c.forCultistId), c.artifactId)
    requester ! FinishedCloning(c, success)
    context.stop(self)
  }

  def failedAttempt(c: Clone) {
    attempted(c, Exit(-1, Nil, -1))
  }

  def escapeString(in: String): String = {
    in // no escaping necessary here
  }
}
