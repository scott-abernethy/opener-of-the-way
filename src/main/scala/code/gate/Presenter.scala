package code.gate

import org.squeryl.PrimitiveTypeMode._
import net.liftweb.common.Loggable
import code.model.Mythos._
import code.model._
import code.comet._

// TODO merge with Cloner

trait Presenter {
  def currently: Option[Presence]
  def start(job: Presence)
  def cancel
}

trait PresenterComponent {
  val presenter: Presenter
}

trait PresenterComponentImpl extends PresenterComponent {
  this: ProcessorComponent with ManipulatorComponent =>

  val presenter = new Presenter() with Loggable {
    private var cur: Option[Presence] = None

    def currently = cur

    def start(presence: Presence) {

      presence.state = PresenceState.presenting
      presence.attempted = T.now
      transaction { presences.insertOrUpdate(presence) }

      cur = Some(presence)

      ArtifactServer ! ArtifactTouched(ArtifactPresenting, presence.artifactId)
      transaction {
        // todo fix with better comprehension
        for {
          src <- presence.artifact.flatMap(_.localPath)
          dest = presence.artifactId.toString
        } yield ("presenter" :: escapeString(src) :: escapeString(dest) ::  Nil)
      } match {
        case Some(command) =>
          processor.process(command).start(result => attempted(presence, result) )
        case None =>
          failedAttempt(presence)
      }
    }

    def cancel {
      // TODO kill the process?
      cur.foreach(failedAttempt _)
    }

    def attempted(p: Presence, result: Result) {
      logger.debug("Presenter result " + result)
      p.state = if (result.success) PresenceState.present else PresenceState.called
      p.attempts = p.attempts + 1
      p.duration = result.duration
      transaction { presences.update(p) }
      cur = None
      if (result.success) {
        Environment.watcher ! 'Sink
      } else {
        Environment.watcher ! PresenceFailed(p)
      }
      ArtifactServer ! ArtifactTouched(if (result.success) ArtifactPresented else ArtifactPresentFailed, p.artifactId)
      manipulator ! Wake
    }

    def failedAttempt(p: Presence) { attempted(p, Result(false, Nil, -1)) }

    def escapeString(in: String): String = in // no escaping necessary here
  }
}
