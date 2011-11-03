package code.model

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import code.gate.T
import net.liftweb.http.js.{JsCmds, JsCmd}
import xml.NodeSeq

// TODO use the same structure in the ClonedSnapshotFactory
// TODO should this really be separate from the ClonedSnapshotFactory?
// TODO have servers and filtered comets for this stuff. It's not scaling well. Even just a single thing that does the db query for artifact to clones...?

abstract class SnapshotAction
case object Add extends SnapshotAction
case object Update extends SnapshotAction
case object Remove extends SnapshotAction

class AwaitingSnapshot(
  val awaiting: List[(Artifact, Option[ArtifactState.Value], Clone)]
)
{
  val acceptedStates = Set(ArtifactState.awaiting, ArtifactState.awaitingPresent, ArtifactState.awaitingLost, ArtifactState.cloning)

  def update(artifact: Artifact, state: Option[ArtifactState.Value], clone: Option[Clone]): (AwaitingSnapshot, SnapshotAction) = {

    val filtered = awaiting.filterNot( i => i._1.id == artifact.id )
    val existed = filtered.size < awaiting.size

    // TODO should remove be done with a remove method
    val include = (state, clone) match {
      case (Some(s), Some(c)) if ( acceptedStates.contains(s) ) => (artifact, state, c) :: Nil
      case _ => Nil
    }

    val updated = filtered ::: include

    val sorted = updated.sortWith((a, b) => a._3.requested.before(b._3.requested))
    val s = new AwaitingSnapshot( sorted )

    // TODO update should take into account whether UI will change...?
    (existed, include) match {
      case (_, Nil) => ( s, Remove )
      case (true, _) => ( s, Update )
      case (false, _) => ( s, Add )
    }
  }
}

class AwaitingSnapshotFactory {
  def create(cultistId: Long): AwaitingSnapshot = {
    val as = inTransaction(awaiting(cultistId))
    new AwaitingSnapshot(stateOf(cultistId, as))
  }

  def awaiting(cultistId: Long) = {
    join(clones, artifacts, presences.leftOuter)((c, a, p) =>
      where(c.forCultistId === cultistId and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
        select((c, a, p))
        orderBy(c.requested desc)
        on(c.artifactId === a.id, c.artifactId === p.map(_.artifactId))
    ).toList
  }

  def stateOf(cultistId: Long, in: List[(Clone, Artifact, Option[Presence])]): List[(Artifact, Option[ArtifactState.Value], Clone)] = {
    for {
      (clone, artifact, presence) <- in
      state = artifact.stateFor(cultistId, cultistId - 1, Some(clone), T.now, presence)
    } yield (artifact, state, clone)
  }

  def stateOf(cultistId: Long, artifactId: Long): Option[(Artifact, Option[ArtifactState.Value], Option[Clone])] = {
    inTransaction {
      join(artifacts, clones.leftOuter, presences.leftOuter)( (a, c, p) =>
        where(a.id === artifactId and (c.map(_.forCultistId).~.isNull or c.map(_.forCultistId) === Some(cultistId)))
        select((a, c, p))
        on(a.id === c.map(_.artifactId), a.id === p.map(_.artifactId))
      ).headOption match {
        case Some( (a, c, p) ) =>
          val state = a.stateFor(cultistId, cultistId - 1, c, T.now, p)
          Some( (a, state, c) )
        case _ =>
          None
      }
    }
  }
}