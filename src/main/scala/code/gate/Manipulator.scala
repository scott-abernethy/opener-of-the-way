package code.gate

import java.util.Calendar
import code.comet._
import code.model._
import code.model.Mythos._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._
import net.liftweb.common._
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query

case object Wake
case class Warn(invalid: Clone)
case object Withdraw

trait ManipulatorComponent {
  val manipulator: Manipulator
  trait Manipulator extends Actor
}

trait ManipulatorComponentImpl extends ManipulatorComponent {
  // cloner ... idle. working on clone x. start | cancel.
  val manipulator = new Manipulator with Loggable {
    val cloner = new Cloner(this)
    val waitings: Query[Clone] = from(clones)(c => where(c.state.is === CloneState.waiting) select(c) orderBy(c.id asc))
    def act() {
      while (true) {
        receive {
          case Wake => 


            // has the current cloner timed out?
            // find cultists who have gateways online
            //Gateway.viableDestinations.toList.map(g => g.cultistId.is -> g)
            // find artifacts that are in gateways online
            val as = Artifact.viableSources.toList
            // get random(?) waiting clone for the above
            from(clones)(c => 
              where(
                c.state.is === CloneState.waiting and
                c.artifactId.is in from(Artifact.viableSources)(a => select(a.id))
              )
              select(c) 
              orderBy(c.id asc)
            )
            if (cloner.currently.isEmpty) waitings.headOption.foreach(cloner.start(_)) 
          case Warn(invalid) => if (cloner.currently.filter(_ == invalid).isDefined) cloner.cancel
          case Withdraw => 
            cloner.cancel
            exit
          case _ =>
        }
      }
    }
  }
}

    /*
    -q quiet
    -t preserve modification times
    --progress
    */
class Cloner(manipulator: Actor) {
  var currently: Option[Clone] = None
  def start(job: Clone) {
    currently = Some(job)
    job.state(CloneState.progressing)
    clones.update(job)
    //val src = 
    //val dest
    //Processor.process("rsync" :: Nil).start()
    manipulator ! Wake
  }
  def cancel {
    currently = None
    manipulator ! Wake
  }
}
