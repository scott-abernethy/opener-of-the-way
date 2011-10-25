package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{ListenerManager, CometActor, CometListener}
import org.squeryl.PrimitiveTypeMode._
import code.model.{CloneState, Mythos}
import net.liftweb.util.ClearClearable
import code.util.DatePresentation

// TODO optimise, shifting data is identical for all sessions

class Shifting extends CometActor with CometListener {

  var average: Long = 0L

  def registerWith = ArtifactServer

  override def lowPriority = {
    // TODO better classification of artifact change required here
    case _ => {
      average = calculate()
      reRender(true)
    }
  }

  def render = {
    ClearClearable &
    ".shifting-average" #> DatePresentation.duration(average)
  }
  
  def calculate(): Long = {
    val completed = transaction {
      from(Mythos.clones)(c =>
        where(c.state === CloneState.cloned)
        select(c)
      ).page(0, 100).toList
    }

    val total = completed.foldLeft(0L){ (sum, clone) =>
      val lag = clone.attempted.getTime - clone.requested.getTime
      if (lag > 0) {
        sum + lag + clone.duration
      } else {
        // Weird
        sum + clone.duration
      }
    }

    if (completed.size > 0) total / completed.size else 0L;
  }
}