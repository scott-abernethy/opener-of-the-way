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
import code.util.{Maintainer, Maintain}
import code.gate.Watcher._
case object Wake
case class Warn(invalid: Clone)
case object Withdraw

trait ManipulatorComponent {
  val manipulator: Manipulator
  trait Manipulator extends Actor
}

trait ManipulatorComponentImpl extends ManipulatorComponent {
  this: ClonerComponent with PresenterComponent =>

  def waitingPresences(): List[Presence] = {
    transaction(
      sourcesQuery()
        .toList
        .filter(x => x._1.state == PresenceState.called && x._2.state == GateState.open)
        .map(_._1)
    )
  }

  def waitingClones(): List[Clone] = {
    transaction(
      sinksQuery()
        .toList
        .filter(x => x._1.state == CloneState.awaiting && x._2.state == GateState.open)
        .map(_._1)
    )
  }

  val manipulator = new Manipulator with Loggable {
    val maintainer = new Maintainer(this, 1 * 60 * 1000L).start
    def act() {
      loop {
        react {
          case Wake =>
            // TODO waking should be a backup mechanism for doing this. Do on demand.

            // has the current cloner timed out?
            // get random(?) waiting clone
            // TODO is this actually in another thread?

            // TODO don't present if no space available!! get summoner to ensure, message goes via them.
            val presences: List[Presence] = waitingPresences()
            logger.debug("Manipulator WAITING presences: " + presences)
            if (presenter.currently.isEmpty) {
              presences.headOption.foreach{ p => presenter.start(p) }
            }
            val clones: List[Clone] = waitingClones()
            logger.debug("Manipulator WAITING clones: " + clones)
            if (cloner.currently.isEmpty) {
              clones.headOption.foreach{ c => cloner.start(c) }
            }

          case Warn(invalid) =>
            if (cloner.currently.filter(_ == invalid).isDefined) cloner.cancel
          
          case Withdraw =>
            maintainer ! Destroy
            presenter.cancel
            cloner.cancel
            exit

          case Ping => reply(Pong)

          case Activate =>
            maintainer ! Activate

          case Maintain =>
            self ! Wake

          case 'Flush =>
            if (presenter.currently.isEmpty) {
              transaction ( update(presences)(p =>
                setAll(p.state := PresenceState.unknown))
              )
            }
            if (cloner.currently.isEmpty) {
              transaction ( update(clones)(c =>
                where(c.state === CloneState.cloning)
                set(c.state := CloneState.awaiting))
              )
            }

          case _ =>
        }
      }
    }
  }
}