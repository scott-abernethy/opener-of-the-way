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

package model

import org.squeryl.PrimitiveTypeMode._
import model.Mythos._
import gate.T

class ClonedSnapshot(
  val cloned: List[(Artifact, Option[ArtifactState.Value])]
)

class ClonedSnapshotFactory {
  def create(cultistId: Long): ClonedSnapshot = {
    val cs = inTransaction(cloned(cultistId))
    new ClonedSnapshot(stateOf(cultistId, cs))
  }

  def cloned(cultistId: Long) = {
    join(clones, artifacts)((c, a) =>
      where(c.forCultistId === cultistId and c.state === CloneState.cloned and c.attempted >= T.ago(7 * 24 * 60 * 60 * 1000))
        select((c, a))
        orderBy(c.attempted.desc)
        on(c.artifactId === a.id)
    ).toList.map( i => (i._1, i._2, None) )
  }

  def stateOf(cultistId: Long, in: List[(Clone, Artifact, Option[Presence])]): List[(Artifact, Option[ArtifactState.Value])] = {
    for {
      (clone, artifact, presence) <- in
      state = artifact.stateFor(cultistId, cultistId - 1, Some(clone), T.now, presence)
    } yield (artifact, state)
  }
}