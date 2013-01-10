package gate

import org.squeryl.PrimitiveTypeMode._
import model.Mythos._
import model._
import comet._
import state._
import akka.actor.{Actor, ActorRef}
import play.api.Logger

// TODO merge with Cloner

case class StartPresenting(job: Presence)
case class FinishedPresenting(job: Presence, success: Boolean)
case object CancelPresenting

class Presenter(val processs: Processs, val watcher: ActorRef, val artifactServer: ActorRef) extends Actor {
  var cur: Option[Presence] = None
  var requester: ActorRef = context.system.deadLetters
  var destroyHandle: Destroyable = new NonDestroyable

  def receive = {
    case StartPresenting(job) if (cur.isEmpty) => {
      requester = sender
      start(job)
    }
    case CancelPresenting => {
      cancel
    }
    case 'Cancel => {
      cancel
    }
    case exit: Exit => {
      cur.foreach(attempted(_, exit))
    }
    case msg => {
      unhandled(msg)
    }
  }

  def start(presence: Presence) {
    Logger.debug(this + " start " + presence)
    presence.state = PresenceState.presenting
    presence.attempted = T.now
    transaction { presences.insertOrUpdate(presence) }

    cur = Some(presence)

    artifactServer ! ArtifactTouched(ArtifactPresenting, presence.artifactId)
    transaction {
      // todo fix with better comprehension
      for {
        src <- presence.artifact.flatMap(_.localPath)
        dest = presence.artifactId.toString
      } yield ("presenter" :: escapeString(src) :: escapeString(dest) ::  Nil)
    } match {
      case Some(command) => {
        val (future, destroyable) = processs.start(command)
        destroyHandle = destroyable
        // TODO?
        import scala.concurrent.ExecutionContext.Implicits.global
        future.onFailure {
          case e: Exception => self ! Exit(-1, e.getMessage :: Nil, -1)
        }
        future.onSuccess {
          case exit: Exit => self ! exit
        }
      }
      case None => {
        failedAttempt(presence)
      }
    }
  }

  def cancel {
    destroyHandle.destroy
    destroyHandle = new NonDestroyable
    cur.foreach(failedAttempt _)
  }

  def attempted(p: Presence, result: Exit) {
    Logger.debug("Presenter result " + result)
    val success = result.exitValue == 0
    p.state = if (success) PresenceState.present else PresenceState.called
    p.attempts = p.attempts + 1
    p.duration = result.duration
    transaction { presences.update(p) }
    cur = None
    if (success) {
      watcher ! 'Sink
    } else {
      watcher ! PresenceFailed(p)
    }
    artifactServer ! ArtifactTouched(if (success) ArtifactPresented else ArtifactPresentFailed, p.artifactId)
    requester ! FinishedPresenting(p, success)
    context.stop(self)
  }

  def failedAttempt(p: Presence) {
    attempted(p, Exit(-1, Nil, -1))
  }

  def escapeString(in: String): String = {
    in // no escaping necessary here
  }
}
