/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package state

import org.squeryl.PrimitiveTypeMode._
import gate.T
import model.{ArtifactState, Artifact, Presence, Clone}
import akka.actor.{ActorRef, Actor}
import play.api.Logger

sealed abstract class ArtifactChange

case object ArtifactCreated extends ArtifactChange
case class ArtifactAwaiting(forCultistId: Long) extends ArtifactChange
case class ArtifactUnawaiting(forCultistId: Long) extends ArtifactChange
case object ArtifactPresenting extends ArtifactChange
case object ArtifactPresented extends ArtifactChange
case object ArtifactPresentFailed extends ArtifactChange
case class ArtifactCloning(forCultistId: Long) extends ArtifactChange
case class ArtifactCloned(forCultistId: Long) extends ArtifactChange
case class ArtifactCloneFailed(forCultistId: Long) extends ArtifactChange

case class ArtifactTouched(change: ArtifactChange, artifactId: Long)

case class ArtifactPack(change: ArtifactChange, artifact: Artifact, ownerId: Long, presence: Option[Presence], clones: List[Clone]) {

  def stateFor(cultistId: Long): Option[ArtifactState.Value] = {
    artifact.stateFor(cultistId, ownerId, cloneFor(cultistId), T.now, presence)
  }

  def cloneCount(): Option[Int] = {
    if (clones.isEmpty) None else Some(clones.size)
  }

  def cloneFor(cultistId: Long): Option[Clone] = {
    clones.find(_.forCultistId == cultistId)
  }
}

class ArtifactServer extends Actor {

  lazy val stream: ActorRef = context.system.actorFor("/user/StateStream")

  def receive = {
    // todo replace this lameness with extractors for change type to logger level
    case ArtifactTouched(ArtifactPresentFailed, id) => fwd(ArtifactPresentFailed, id, Logger.warn(_))
    case ArtifactTouched(ArtifactCloneFailed(c), id) => fwd(ArtifactCloneFailed(c), id, Logger.warn(_))
    case ArtifactTouched(ArtifactCreated, id) => fwd(ArtifactCreated, id, Logger.info(_))
    case ArtifactTouched(ArtifactAwaiting(c), id) => fwd(ArtifactAwaiting(c), id, Logger.info(_))
    case ArtifactTouched(ArtifactCloned(c), id) => fwd(ArtifactCloned(c), id, Logger.info(_))
    case ArtifactTouched(change, id) => fwd(change, id, Logger.debug(_))
  }

  def fwd(change: ArtifactChange, id: Long, logMethod: (String) => Unit) {
    import model.Mythos._
    val results = inTransaction(
      join(artifacts, gateways, presences.leftOuter, clones.leftOuter)( (a,g,p,c) =>
        where(a.id === id)
        select( (a,g.cultistId,p,c) )
        on(a.gatewayId === g.id, a.id === p.map(_.artifactId), a.id === c.map(_.artifactId))
      ).toList
    )
    val combined: Seq[(Artifact, Long, Option[Presence], List[Clone])] = results.foldRight(List.empty[(Artifact, Long, Option[Presence], List[Clone])]){ (in: (Artifact, Long, Option[Presence], Option[Clone]), out: List[(Artifact, Long, Option[Presence], List[Clone])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3, in._4.toList ::: head._4)) :: tail
        case list =>
          ((in._1, in._2, in._3, in._4.toList)) :: list
      }
    }
    combined.headOption match {
      case Some( (a, ownerId, p, cs) ) => {
        val pack: ArtifactPack = ArtifactPack(change, a, ownerId, p, cs)
        logMethod(pack.change + " -> " + a)
        stream ! pack
      }
      case _ => {
        Logger.warn("Argh")
      }
    }
  }

}