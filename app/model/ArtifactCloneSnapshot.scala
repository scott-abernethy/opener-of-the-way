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
import java.util.{TimeZone, Date}
import gate.T
import collection.immutable.{HashMap, TreeMap}
import java.text.{SimpleDateFormat}
import _root_.util.{Context, DatePresentation}
import scala.concurrent._
import scala.concurrent.Future

case class ArtifactCloneInfo(state: ArtifactState.Value, clones: Int)

object ArtifactCloneSnapshot {
  def apply(cultistId: Long, after: Option[Long], count: Int): Future[ArtifactCloneSnapshot] = {
    future {
      val snapshot = new ArtifactCloneSnapshot(count, after.getOrElse(Long.MaxValue))
      snapshot.reload(cultistId)
      snapshot
    }(Context.dbOperations)
  }
}

class ArtifactCloneSnapshot(val count: Int, val after: Long) {

  var currentCultistId: Long = -1L
  var items: TreeMap[String, List[Artifact]] = new TreeMap[String, List[Artifact]]
  var states: Map[Long, ArtifactCloneInfo] = _

  def stateFor(artifactId: Long): Option[ArtifactState.Value] = {
    states.get(artifactId).map(_.state)
  }

  def clonesFor(artifactId: Long): Option[Int] = {
    states.get(artifactId).map(_.clones)
  }

  def reload(cultistId: Long) {
    currentCultistId = cultistId
    items = new TreeMap[String, List[Artifact]]
    states = new HashMap[Long, ArtifactCloneInfo]
    val results: List[(Artifact, Long, Option[Clone], Option[Presence])] = inTransaction(join(artifacts, gateways, clones.leftOuter, presences.leftOuter)((a, g, c, p) =>
      where((a.witnessed > T.ago(Artifact.goneAfter)) and (a.id.~ < after))
      select((a, g.cultistId, c, p))
      orderBy(a.id.desc)
      on(a.gatewayId === g.id, a.id === c.map(_.artifactId), a.id === p.map(_.artifactId))
    ).page(0, count).toList)
    val combined: Seq[(Artifact, Long, List[Clone], Option[Presence])] = results.foldRight(List.empty[(Artifact, Long, List[Clone], Option[Presence])]){ (in: (Artifact, Long, Option[Clone], Option[Presence]), out: List[(Artifact, Long, List[Clone], Option[Presence])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3.toList ::: head._3, in._4)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList, in._4)) :: list
      }
    }
    combined.reverse.foreach{ i =>
      val artifact: Artifact = i._1
      items = insertItem(items, artifact)
      parseState(artifact, cultistId, i._2, i._3, i._4).foreach(s =>
        states = states + ((artifact.id, ArtifactCloneInfo(s, i._3.size)))
      )
    }
  }

  private def insertItem(into: TreeMap[String, List[Artifact]], a: Artifact): TreeMap[String, List[Artifact]] = {
    discoveredGroup(a) match {
      case Some(key) =>
        val as = into.getOrElse(key.toString, Nil)
        if (!as.contains(a)) into + ((key.toString, a :: as)) else into
      case other => into
    }
  }

  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone], presence: Option[Presence]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now, presence)
  }

  private def discoveredGroup(a: Artifact): Option[String] = {
    for {
      timestamp <- Option(a.discovered)
      time = timestamp.getTime
    }
    yield DatePresentation.yearMonthDay(time)
  }
}