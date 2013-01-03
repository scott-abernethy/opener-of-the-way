package model

import org.squeryl.PrimitiveTypeMode._
import model.Mythos._
import gate.T
import xml.NodeSeq

// TODO use the same structure in the ClonedSnapshotFactory
// TODO should this really be separate from the ClonedSnapshotFactory?
// TODO have servers and filtered comets for this stuff. It's not scaling well. Even just a single thing that does the db query for artifact to clones...?

abstract class SnapshotAction
case class Add(cloneId: Long) extends SnapshotAction
case class Update(cloneId: Long) extends SnapshotAction
case class Remove(cloneId: Long) extends SnapshotAction
case object Nothing extends SnapshotAction
case object Other extends SnapshotAction

class AwaitingSnapshot(
  val awaiting: List[(Artifact, Option[ArtifactState.Value], Clone)]
)
{
  val acceptedStates = Set(ArtifactState.awaiting, ArtifactState.awaitingPresent, ArtifactState.awaitingLost, ArtifactState.cloning)

  def update(artifact: Artifact, state: Option[ArtifactState.Value], clone: Option[Clone]): (AwaitingSnapshot, SnapshotAction) = {

    val (removed, filtered) = awaiting.partition( i => i._1.id == artifact.id )

    // TODO should remove be done with a remove method
    val include = (state, clone) match {
      case (Some(s), Some(c)) if ( acceptedStates.contains(s) ) => (artifact, state, c) :: Nil
      case _ => Nil
    }

    val updated = filtered ::: include

    val sorted = updated.sortWith((a, b) => a._3.requested.before(b._3.requested))
    val s = new AwaitingSnapshot( sorted )

    // TODO update should take into account whether UI will change...?
    (removed, include) match {
      case (Nil, Nil) => ( s, Nothing )
      case (List(existing), Nil) => ( s, Remove(existing._3.id) )
      case (List(existing), List(update)) => ( s, Update(update._3.id) )
      case (Nil, List(update)) => ( s, Add(update._3.id) )
      case _ => ( s, Other )
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

//  def stateOf(cultistId: Long, artifactId: Long): Option[(Artifact, Option[ArtifactState.Value], Option[Clone])] = {
//    inTransaction {
//      for {
//        a <- artifacts.lookup(artifactId)
//        p = presences.where(p => p.artifactId === artifactId).headOption
//        c = clones.where(c => c.artifactId === artifactId and c.forCultistId === cultistId).headOption
//        state = a.stateFor(cultistId, cultistId - 1, c, T.now, p)
//      }
//      yield (a, state, c)
//    }
//  }
}