package gate

import akka.actor.{Actor, ActorRef}
import model._
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import java.io.File
import play.api.Logger
import util.Size

case class Summon(artifactId: Long)

object Summoner {

  // TODO Merge with Watcher sources query?
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
        (p.state === PresenceState.present or p.state === PresenceState.unknown or p.state === PresenceState.released)
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

  def presenceToClean(): Query[Presence] = {
    from(presences)( p =>
      where( p.state === PresenceState.released )
      select( p )
    )
  }
}

class Summoner(lurker: ActorRef, watcher: ActorRef, keepers: ActorRef) extends Actor {
  import Summoner._
  import KeeperRouterApi._
  import KeeperApi._

  def receive = {

    // TODO don't add / summon it if no room.

    case 'Wake => {
      // TODO do called yet not needed and not possible presences ever get released? Don't think so.
      clean()
      for ( clone <- transaction( requestedPresences().toList ) ) {
        call(clone.artifactId)
      }
      releaseIfNecessary()
    }
    case Summon(artifactId) => {
      call(artifactId)
      releaseIfNecessary()
    }
    case 'Check => {
      // called > ...
      // presenting > called
      // present > [check]
      // unknown > ...
      // released > ...
      transaction {
        update(presences)(p =>
          where(p.state === PresenceState.presenting)
          set(p.state := PresenceState.called)
        )
        from(presences)(p =>
          where(p.state === PresenceState.present)
          select(p)
        ).toList.foreach { p =>
          val localFile = new File(p.localPath)
          // TODO check against artifact size also
          if ( !localFile.exists() ) {
            // Assume the presence is gone
            presences.deleteWhere( x => x.id === p.id )
          }
        }
      }
      sender ! 'CheckComplete
    }
    case 'Ping => {
      sender ! 'Pong
    }
    case msg => {
      unhandled(msg)
    }
  }

  private def call(artifactId: Long) {
    transaction {
      Presence.withArtifact(artifactId).headOption match {
        case Some((artifact, Some(presence))) if presence.state == PresenceState.unknown => {
          presence.state = PresenceState.called
          presences.update(presence)
          logSummonerInfo()
          watcher ! 'Source
          keepers ! ToKeeper(artifact.gatewayId, Admit(presence :: Nil))
        }
        case Some((artifact, Some(presence))) if presence.state == PresenceState.present => {
          // TODO is that necessary?
          watcher ! 'Sink
          // TODO don't use a shotgun
          keepers ! ToAll(Release(Nil))
        }
        case Some((artifact, None)) => {
          val p = new Presence()
          p.artifactId = artifactId
          p.state = PresenceState.called
          presences.insert(p)
          logSummonerInfo()
          watcher ! 'Source
          keepers ! ToKeeper(artifact.gatewayId, Admit(p :: Nil))
        }
        case _ =>
      }
    }
  }

  private def releaseIfNecessary() {
    transaction {
      val ps = presentByPurgeCombined()
      var length = ps.foldLeft(0L)( (sum,i) => sum + i._2.length )

      val releasable = ps.filter( i => !isAwaiting(i._3) ).filter( i => i._1.state != PresenceState.released )

      val release = releasable.takeWhile{ i =>
        val take = length > Presence.maxPresenceLength
        length = length - i._2.length
        take
      }

      release.foreach { i =>
        val presence = i._1
        presence.state = PresenceState.released
        presences.update( presence )
      }

      if (release.size > 0) logSummonerInfo()
    }
  }

  private def isAwaiting(clone: List[Clone]): Boolean = {
    clone.map(_.state).exists(s => s == CloneState.awaiting || s == CloneState.cloning)
  }

  private def clean() {
    for (p <- transaction( presenceToClean().toList )) {
      // TODO do this in separate thread...
      val localFile = new File(p.localPath)
      if ( !localFile.exists() || localFile.delete() ) {
        transaction( presences.deleteWhere( x => x.id === p.id ) )
      }
    }
  }

  private def logSummonerInfo() {
    val ps = presentByPurgeCombined()
    var length = ps.foldLeft(0L)( (sum,i) => sum + i._2.length )
    Logger.debug("Summoner LENGTH: " + length + " = " + Size.printGiB(length))
  }
}