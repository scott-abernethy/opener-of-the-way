package code.model

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import code.gate.T

class CloneSnapshot(
  val awaiting: List[(Artifact, Option[ArtifactState.Value])],
  val cloned: List[(Artifact, Option[ArtifactState.Value])]
)
class CloneSnapshotFactory {
  def create(cultistId: Long): CloneSnapshot = {
    // todo compute in parallel...
    val (as, cs) = inTransaction((awaiting(cultistId), cloned(cultistId)))
    new CloneSnapshot(stateOf(cultistId, as), stateOf(cultistId, cs))
  }
  def awaiting(cultistId: Long) = {
    join(clones, artifacts)((c, a) =>
      where(c.forCultistId === cultistId and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
        select((c, a))
        orderBy(c.requested desc)
        on(c.artifactId === a.id)
    ).toList
  }
  def cloned(cultistId: Long) = {
    join(clones, artifacts)((c, a) =>
      where(c.forCultistId === cultistId and c.state === CloneState.cloned and c.attempted >= T.ago(7 * 24 * 60 * 60 * 1000))
        select((c, a))
        orderBy(c.attempted desc)
        on(c.artifactId === a.id)
    ).toList
  }
  def stateOf(cultistId: Long, in: List[(Clone, Artifact)]): List[(Artifact, Option[ArtifactState.Value])] = {
    for {
      (clone, artifact) <- in
      state = artifact.stateFor(cultistId, cultistId - 1, Some(clone), T.now)
    } yield (artifact, state)
  }
}