package code.gate

import akka.actor.{Actor, ActorRef}
import net.liftweb.common.Loggable
import code.model._
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query

case class Summon(artifactId: Long)

object Summoner {
  def requestedPresences(): Query[Clone] = {
    join(clones, presences.leftOuter)( (c, p) =>
      where(
        c.state === CloneState.awaiting and
        (p.map(_.state).~.isNull or p.get.state === PresenceState.unknown)
      )
      select( c )
      on( c.artifactId === p.map(_.artifactId) )
    )
  }

  def presentByPurge(): Query[(Presence, Artifact, Option[Clone])] = {
    join(presences, artifacts, clones.leftOuter)( (p, a, c) =>
      where(
        (p.state === PresenceState.present or p.state === PresenceState.unknown)
      )
      select( (p, a, c) )
      orderBy( a.discovered asc, a.id asc )
      on( p.artifactId === a.id, p.artifactId === c.map(_.artifactId) )
    )
  }

  def presentByPurgeCombined(): List[(Presence, Artifact, List[Clone])] = {
    val results = presentByPurge().toList
    results.foldRight(List.empty[(Presence, Artifact, List[Clone])]){ (in: (Presence, Artifact, Option[Clone]), out: List[(Presence, Artifact, List[Clone])]) =>
      out match {
        case head :: tail if (head._2 == in._2) =>
          ((in._1, in._2, in._3.toList ::: head._3)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList)) :: list
      }
    }

  }
}

class Summoner(lurker: scala.actors.Actor) extends Actor with Loggable {
  import Summoner._

  def receive = {
    case 'Wake => {
      for ( clone <- transaction( requestedPresences().toList ) ) {
        self ! Summon(clone.artifactId)
      }
      releaseIfNecessary()
    }
    case Summon(artifactId) => {
      transaction {
        Presence.forArtifact(artifactId).headOption match {
          case Some(presence) => {
            presence.state = PresenceState.called
            presences.update(presence)
          }
          case None => {
            val p = new Presence()
            p.artifactId = artifactId
            p.state = PresenceState.called
            presences.insert(p)
          }
        }
      }
      releaseIfNecessary()
    }
    case 'Ping => {
      self.reply( 'Pong )
    }
  }

  private def releaseIfNecessary() {
    transaction {
      val ps = presentByPurgeCombined()
      val length = ps.foldLeft(0L)( (sum,i) => sum + i._2.length )
      if (length > Presence.maxPresenceLength) {
        val release = ps.filter(i => !isAwaiting(i._3)).headOption match {
          case Some((presence, _, _)) =>
            presence.state = PresenceState.released
            presences.update( presence )
          case _ =>
        }
      }
      // TODO delete more than one at a time?
    }
  }

  private def isAwaiting(clone: List[Clone]): Boolean = {
    clone.map(_.state).exists(s => s == CloneState.awaiting || s == CloneState.cloning)
  }
}