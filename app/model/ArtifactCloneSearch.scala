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

import collection.immutable.{HashMap, TreeMap}
import java.util.{Date, TimeZone}
import gate.T
import org.squeryl.PrimitiveTypeMode._
import model.Mythos._

class ArtifactCloneSearchFactory {
  def create(cultistId: Long, search: String): Seq[(Artifact, Option[ArtifactState.Value], Option[Int])] = {
    val formattedSearch = formatSearch(search)
    val results: List[(Artifact, Long, Option[Clone], Option[Presence])] = inTransaction(join(artifacts, gateways, clones.leftOuter, presences.leftOuter)((a, g, c, p) =>
      where((a.witnessed > T.ago(Artifact.goneAfter)) and (a.path like formattedSearch))
      select((a, g.cultistId, c, p))
      orderBy(a.path.asc)
      on(a.gatewayId === g.id, a.id === c.map(_.artifactId), a.id === p.map(_.artifactId))
    ).toList)
    val combined: Seq[(Artifact, Long, List[Clone], Option[Presence])] = results.foldRight(List.empty[(Artifact, Long, List[Clone], Option[Presence])]){ (in: (Artifact, Long, Option[Clone], Option[Presence]), out: List[(Artifact, Long, List[Clone], Option[Presence])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3.toList ::: head._3, in._4)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList, in._4)) :: list
      }
    }
    for {
      (artifact, ownerId, clones, presence) <- combined
      state = parseState(artifact, cultistId, ownerId, clones, presence)
      count = Some(clones.size)
    } yield (artifact, state, count)
  }
  
  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone], presence: Option[Presence]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now, presence)
  }

  def formatSearch(in: String): String = {
    val out: Option[String] = for {
      s <- Option(in)
      surrounded = "%" + s + "%"
    } yield surrounded.replaceAll("[ *]", "%").replaceAll("[%]+", "%")

    out.getOrElse("%")
  }
}