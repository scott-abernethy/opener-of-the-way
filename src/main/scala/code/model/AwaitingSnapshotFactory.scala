package code.model

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import code.gate.T

class AwaitingSnapshot(
  val awaiting: List[(Artifact, Option[ArtifactState.Value], Clone)]
)

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
}