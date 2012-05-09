package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{ListenerManager, CometActor, CometListener}
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.util.ClearClearable
import code.util.DatePresentation
import code.model.{Clone, CloneState, Mythos}
import code.state.ArtifactServer

// TODO optimise, shifting data is identical for all sessions

object Shifting {
  def durationOf(clone: Clone): Long = {
    clone.waitPlusDuration()
  }

  def calculateMedian(in: List[Clone]): Long = {
    val durations = in.map(durationOf(_))
    if (durations.size > 0) {
      durations.sorted.apply(durations.size / 2)
    } else {
      0L
    }
  }

  def calculateMean(in: List[Clone]): Long = {
    val total = in.foldLeft(0L){ (sum, clone) =>
      sum + durationOf(clone)
    }

    if (in.size > 0) total / in.size else 0L;
  }
}

class Shifting extends CometActor with CometListener {

  var average: Long = 0L

  def registerWith = ArtifactServer

  override def lowPriority = {
    // TODO better classification of artifact change required here would allow for calculating a rolling average
    case _ => {
      average = Shifting.calculateMedian(recentlyCompleted())
      reRender(true)
    }
  }

  def render = {
    ClearClearable &
    ".shifting-average" #> DatePresentation.duration(average)
  }

  def recentlyCompleted(): List[Clone] = {
    transaction {
      from(Mythos.clones)(c =>
        where(c.state === CloneState.cloned)
        select(c)
        orderBy(c.attempted desc)
      ).page(0, 100).toList
    }
  }
}