package code.gate

import org.squeryl.PrimitiveTypeMode._
import net.liftweb.common.Loggable
import code.comet.ArtifactUpdated._
import code.comet.{ArtifactUpdated, ArtifactServer}
import code.model.Mythos._
import code.model._

// TODO merge with Cloner

trait Presenter {
  def currently: Option[Presence]
  def start(job: Clone)
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

    def start(clone: Clone) {
      // TODO work of presence table only, ignore clones table.
      
      val presence = transaction {
        from(presences)(p =>
          where(p.artifactId === clone.artifactId)
          select(p)
        ).headOption.getOrElse(Presence.create(clone.artifactId))
      }

      presence.state = PresenceState.presenting
      presence.attempted = T.now
      transaction { presences.insertOrUpdate(presence) }

      cur = Some(presence)

      ArtifactServer ! ArtifactUpdated(presence.artifactId)
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
      if (!result.success) Environment.watcher ! PresenceFailed(p)
      ArtifactServer ! ArtifactUpdated(p.artifactId)
      manipulator ! Wake
    }

    def failedAttempt(p: Presence) { attempted(p, Result(false, Nil, -1)) }

    def escapeString(in: String): String = in // no escaping necessary here
  }
}
