package code.gate

import java.util.Calendar
import code.comet._
import code.model._
import code.model.Mythos._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import net.liftweb.common._
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._

trait Cloner {
  def currently: Option[Clone]
  def start(job: Clone)
  def cancel
}

trait ClonerComponent {
  val cloner: Cloner
}

    /*
    -q quiet
    -t preserve modification times
    --progress
    */
trait ClonerComponentImpl extends ClonerComponent {
  this: ProcessorComponent with ManipulatorComponent =>

  val cloner = new Cloner() with Loggable {
    private var cur: Option[Clone] = None

    def currently = cur

    def start(job: Clone) {
      cur = Some(job)
      job.state = CloneState.cloning
      job.attempted = T.now
      transaction { clones.update(job) }
      ArtifactServer ! ArtifactUpdated(job.artifactId)
      transaction {
        // todo fix with better comprehension
        for {
          src <- job.artifact.flatMap(_.localPath)
          dest <- job.forCultist.flatMap(_.destination).map(_.clonesPath)
        } yield ("cloner" :: escapeString(src) :: escapeString(dest) ::  Nil)
      } match {
        case Some(command) =>
          processor.process(command).start(result => attempted(job, result) )
        case None =>
          failedAttempt(job)
      }
    }

    def cancel {
      // TODO kill the process?
      cur.foreach(failedAttempt _)
    }

    def attempted(c: Clone, result: Result) {
      logger.debug("Process result " + result)
      c.state = if (result.success) CloneState.cloned else CloneState.awaiting
      c.attempts = c.attempts + 1
      c.duration = result.duration
      transaction { clones.update(c) }
      cur = None
      if (!result.success) Environment.watcher ! CloneFailed(c)
      ArtifactServer ! ArtifactUpdated(c.artifactId)
      manipulator ! Wake
    }

    def failedAttempt(c: Clone) { attempted(c, Result(false, Nil, -1)) }

    def escapeString(in: String): String = in // no escaping necessary here
  }
}
