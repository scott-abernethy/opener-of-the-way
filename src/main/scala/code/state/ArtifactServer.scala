package code.state

import net.liftweb.common.Loggable
import net.liftweb.actor.LiftActor
import net.liftweb.http.ListenerManager
import org.squeryl.PrimitiveTypeMode._
import code.gate.T
import code.model.{ArtifactState, Artifact, Presence, Clone}

sealed class ArtifactChange

case object ArtifactCreated extends ArtifactChange
case class ArtifactRefresh(selectCultistId: Option[Long]) extends ArtifactChange
case object ArtifactAwaiting extends ArtifactChange
case object ArtifactUnawaiting extends ArtifactChange
case object ArtifactPresenting extends ArtifactChange
case object ArtifactPresented extends ArtifactChange
case object ArtifactPresentFailed extends ArtifactChange
case object ArtifactCloning extends ArtifactChange
case object ArtifactCloned extends ArtifactChange
case object ArtifactCloneFailed extends ArtifactChange

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

object ArtifactServer extends LiftActor with ListenerManager with Loggable {

  var createUpdate: AnyRef = "ignore"

  override def lowPriority = {
    case ArtifactTouched(ArtifactPresentFailed, id) => fwd(ArtifactPresentFailed, id, logger.info(_))
    case ArtifactTouched(ArtifactCloneFailed, id) => fwd(ArtifactCloneFailed, id, logger.warn(_))
    case ArtifactTouched(ArtifactCreated, id) => fwd(ArtifactCreated, id, logger.info(_))
    case ArtifactTouched(ArtifactAwaiting, id) => fwd(ArtifactAwaiting, id, logger.info(_))
    case ArtifactTouched(ArtifactCloned, id) => fwd(ArtifactCloned, id, logger.info(_))
    case ArtifactTouched(change, id) => fwd(change, id, logger.debug(_))
    case other => {
      logger.warn("Unexpected " + other)
    }
  }

  // need to distribute, artifact, for each cultist, a.stateFor(c) ... clone count

  // count of clones
  // present?
  // cloned state for each cultist? -> gives count clones . unnecessary though?

  def fwd(change: ArtifactChange, id: Long, logMethod: (String) => Unit) {
    import code.model.Mythos._
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
        logMethod(a + " -> " + pack.change)
        updateListeners(pack)
      }
      case _ => {
        logger.warn("Argh")
      }
    }
  }

}