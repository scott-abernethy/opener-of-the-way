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
      logger.info("Start " + job)
      cur = Some(job)
      job.state = CloneState.progressing
      transaction { clones.update(job) }
      transaction {
        for {
          src <- job.artifact.flatMap(_.localPath)
          dest <- job.forCultist.flatMap(_.destination).map(_.clonesPath)
        } yield ("cloner" :: src :: dest ::  Nil)
      } match {
        case Some(command) =>
          processor.process(command).start((success, messages) => {
            logger.info("Process result " + success + " > " + messages + " >> " + command)
            if (success) {
              successfulAttempt(job)
            } else {
              failedAttempt(job)
            }
          })
        case None =>
          failedAttempt(job)
      }
    }
    def cancel {
      cur.foreach(failedAttempt _)
    }
    def successfulAttempt(c: Clone) {
      c.state = CloneState.done
      transaction { clones.update(c) }
      cur = None
      manipulator ! Wake
    }
    def failedAttempt(c: Clone) {
      c.state = CloneState.waiting
      c.attempts = c.attempts + 1
      transaction { clones.update(c) }
      cur = None
      manipulator ! Wake
    }
  }
}
