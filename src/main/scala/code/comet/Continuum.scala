package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{CometListener, CometActor, ListenerManager}
import net.liftweb.util.ClearClearable
import org.squeryl.PrimitiveTypeMode._
import code.util.Size
import code.gate.T
import code.state._
import java.sql.Timestamp
import code.model.{Artifact, Clone, CloneState, Mythos}
import net.liftweb.http.js.JsCmds
import xml.Text

// todo same for all cultists, simplify

object Continuum {
  def allAwaiting(): Map[Long,(Long,Int)] = {
    val awaiting = inTransaction {
      join(Mythos.clones, Mythos.artifacts)( (c,a) =>
        where(c.state <> CloneState.cloned)
        select( (a.id, a.length, c.id) )
        orderBy( a.id )
        on(c.artifactId === a.id)
      ).toList
    }
    awaiting.foldLeft(List.empty[(Long,Long,Int)]){ (list, i) =>
      list match {
        case (artifactId, length, count) :: tail if (artifactId == i._1) => {
          (artifactId, length, count + 1) :: tail
        }
        case other => {
          (i._1, i._2, 1) :: other
        }
      }
    }.map( x => (x._1, (x._2, x._3)) ).toMap
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

  var loaded = T.startOfDay()
  var awaiting = Continuum.allAwaiting()
  var cloned: Long = Continuum.allClonedToday()

  override def lowPriority = {
    case ArtifactPack(ArtifactAwaiting(_), a, _, _, clones) => {
      updateAwaiting(a, clones)
      reloadClonedIfNextDay()
      renderPartial()
    }
    case ArtifactPack(ArtifactUnawaiting(_), a, _, _, clones) => {
      updateAwaiting(a, clones)
      reloadClonedIfNextDay()
      renderPartial()
    }
    case ArtifactPack(ArtifactCloned(_), a, _, _, clones) => {
      updateAwaiting(a, clones)
      cloned = cloned + a.length
      reloadClonedIfNextDay()
      renderPartial()
    }
    case _ if (reloadClonedIfNextDay()) => {
      renderPartial()
    }
    case _ => {}
  }

  def updateAwaiting(artifact: Artifact, clones: List[Clone]) {
    val count = clones.filter(_.state != CloneState.cloned).size
    if (count > 0) {
      awaiting = awaiting + (artifact.id -> (artifact.length, count))
    }
    else {
      awaiting = awaiting - artifact.id
    }
  }

  def reloadClonedIfNextDay(): Boolean = {
    val day: Timestamp = T.startOfDay()
    if (day.after(loaded)) {
      loaded = day
      cloned = Continuum.allClonedToday()
      true
    }
    else {
      false
    }
  }

  def awaitingText(): String = {
    val length: Long = awaiting.values.foldLeft(0L)( (total,i) => total + (i._1 * i._2) )
    if (length > 0) Size.short(length) else "Empty"
  }

  def clonedText(): String = {
    if (cloned > 0) Size.short(cloned) else "None"
  }

  def render = {
    ClearClearable &
    "#all-awaiting *" #> awaitingText() &
    "#all-cloned *" #> clonedText()
  }

  def renderPartial() {
    partialUpdate(
      JsCmds.SetHtml("all-awaiting", Text(awaitingText())) &
      JsCmds.SetHtml("all-cloned", Text(clonedText()))
    )
  }
}
