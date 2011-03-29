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
  val waitings: Query[Clone] = from(clones)(c =>
              where(
                (c.state === CloneState.queued) and
                (c.forCultistId in from(Gateway.viableDestinations)(g => select(g.cultistId))) and
                (c.artifactId in from(Artifact.viableSources)(a => select(a.id)))
              )
              select(c)
              orderBy(c.attempts asc, c.id asc)
            )
  val manipulator = new Manipulator with Loggable {
    def act() {
      loop {
        react {
          case Wake =>
            // has the current cloner timed out?
            // get random(?) waiting clone
            // checked clone was not attempted very recently, once per 60 seconds should be enough.
            // todo is this actually in another thread?
            if (cloner.currently.isEmpty) {
              val cs = transaction(waitings.toList)
              cs.filter(_.attempted.before(T.ago(60000))).headOption.foreach{c => cloner.start(c)}
            }
          case Warn(invalid) => if (cloner.currently.filter(_ == invalid).isDefined) cloner.cancel
          case Withdraw => 
            cloner.cancel
            exit
          case Ping => reply(Pong)
          case _ =>
        }
      }
    }
  }
}