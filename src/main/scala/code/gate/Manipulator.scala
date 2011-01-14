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

case object Wake
case class Warn(invalid: Clone)
case object Withdraw

trait ManipulatorComponent {
  val manipulator: Manipulator
  trait Manipulator extends Actor
}

trait ManipulatorComponentImpl extends ManipulatorComponent {
  this: ClonerComponent =>
  val manipulator = new Manipulator with Loggable {
    def act() {
      while (true) {
        receive {
          case Wake => 


            // has the current cloner timed out?
            // find cultists who have gateways online
            //Gateway.viableDestinations.toList.map(g => g.cultistId.is -> g)
            // find artifacts that are in gateways online
        //    val as = Artifact.viableSources.toList
            // get random(?) waiting clone for the above
            val waitings: Query[Clone] = from(clones)(c =>
              where(
                (c.state === CloneState.waiting) and
                (c.forCultistId in from(Gateway.viableDestinations)(g => select(g.cultistId)))
//                c.artifactId in from(Artifact.viableSources)(a => select(a.id))
              )
              select(c)
              orderBy(c.id asc)
            )
            if (cloner.currently.isEmpty) transaction{
              waitings.headOption.foreach{c => cloner.start(c)}
            }
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
  val cloner = new Cloner() {
    private var c: Option[Clone] = None
    def currently = c
    def start(job: Clone) {
      c = Some(job)
      job.state = CloneState.progressing
      clones.update(job)
      //val src =
      //val dest
      //Processor.process("rsync" :: Nil).start()
    }
    def cancel {
      c = None
    }
  }
}
