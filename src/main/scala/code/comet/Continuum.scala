package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{CometListener, CometActor, ListenerManager}
import net.liftweb.util.ClearClearable
import code.state.ArtifactServer
import org.squeryl.PrimitiveTypeMode._
import code.model.{CloneState, Mythos}
import code.util.Size
import code.gate.T

// todo same for all cultists, simplify

object Continuum {
  def allAwaiting(): Long = {
    val bytes = inTransaction {
      join(Mythos.clones, Mythos.artifacts)( (c,a) =>
        where(c.state <> CloneState.cloned)
        select(a.length)
        on(c.artifactId === a.id)
      ).toList
    }
    bytes.fold(0L)(_ + _)
  }

  def allClonedToday(): Long = {
    val begin = T.startOfDay(T.now)
    val bytes = inTransaction {
      join(Mythos.clones, Mythos.artifacts)( (c,a) =>
        where(c.state === CloneState.cloned and c.attempted > begin)
        select(a.length)
        on(c.artifactId === a.id)
      ).toList
    }
    bytes.fold(0L)(_ + _)
  }
}

class Continuum extends CometActor with CometListener {
  def registerWith = ArtifactServer

  // load, today, last two days, week.

  override def lowPriority = {
    case _ => {
      reRender()
    }
  }

  def render = {
    val awaiting = Continuum.allAwaiting()
    val cloned = Continuum.allClonedToday()
    ClearClearable &
    ".all-awaiting" #> (if (awaiting > 0) Size.short(awaiting) else "Empty") &
    ".all-cloned" #> (if (cloned > 0) Size.short(cloned) else "None")
  }
}
