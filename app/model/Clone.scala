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

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import gate.T
import util.FutureTransaction._
import concurrent.Future
import concurrent.duration._

class Clone extends MythosObject {
  var artifactId: Long = 0
  var forCultistId: Long = 0
  var state: CloneState.Value = CloneState.awaiting
  var attempts: Long = 0
  var requested: Timestamp = T.now
  var attempted: Timestamp = T.yesterday
  var duration: Long = -2
  var repeats: Long = 0
  
  def artifact: Option[Artifact] = artifactToClones.right(this).headOption
  def forCultist: Option[Cultist] = cultistToClones.right(this).headOption

  def waitPlusDuration(): Long = {
    state match {
      case CloneState.cloned => {
        val lag = attempted.getTime - requested.getTime
        if (lag > 0) {
          lag + duration
        } else {
          // Weird
          duration
        }
      }
      case _ => {
        T.now.getTime - requested.getTime
      }
    }
  }

  override def toString = {
    "Clone[a" + artifactId + " for" + forCultistId + " =" + state + "]"
  }
}

object Clone {
  
  def create(artifactId: Long, forCultistId: Long, state: CloneState.Value) = {
    val c = new Clone
    c.artifactId = artifactId
    c.forCultistId = forCultistId
    c.state = state
    c
  }

  def fake(artifactId: Long, forCultistId: Long, state: CloneState.Value, requested: Timestamp, attempted: Timestamp, duration: Long = -2) = {
    val c = new Clone
    c.artifactId = artifactId
    c.forCultistId = forCultistId
    c.state = state
    c.requested = requested
    c.attempted = attempted
    c.duration = duration
    c
  }

  lazy val nonRepeatableBefore = 30 * 60 * 1000L
  lazy val marginalWaitAfter = 30 * 60 * 1000L
  lazy val poorWaitAfter = 4 * 60 * 60 * 1000L
  lazy val terribleWaitAfter = 3 * 24 * 60 * 60 * 1000L
  lazy val purgeAfter = 60.days.toMillis

  def queue(): Future[List[(Clone, Option[Presence], Artifact, String, String)]] = {
    inFutureTransaction (
      join(clones, presences.leftOuter, artifacts, pseudonyms, cultists)( (c, p, a, n, w) =>
        where(c.state <> CloneState.cloned)
        select( (c, p, a, n.name, w.email) )
        orderBy(c.requested asc)
        on(c.artifactId === p.map(_.artifactId), c.artifactId === a.id, c.forCultistId === n.id, c.forCultistId === w.id)
      ).toList
    )
  }

  def queueSummary(): Future[List[(Clone, Artifact)]] = {
    inFutureTransaction (
      join(clones, artifacts)( (c, a) =>
        where(c.state <> CloneState.cloned)
        select( (c, a) )
        on(c.artifactId === a.id)
      ).toList
    )
  }

  def complete(after: Timestamp): Future[List[(Clone, Artifact)]] = {
    inFutureTransaction (
      join(clones, artifacts)( (c, a) =>
        where( c.state === CloneState.cloned and c.attempted > after )
        select( (c, a) )
        orderBy( c.attempted desc )
        on( c.artifactId === a.id )
      ).toList
    )
  }

  def lastRequest(): Future[Map[Long, Option[Timestamp]]] = {
    inFutureTransaction {
      from(clones)( c =>
        groupBy( c.forCultistId )
        compute( max(c.requested) )
      ).map( group =>
        (group.key, group.measures)
      ).toList.toMap
    }
  }

  def lastClone(): Future[Map[Long, Option[Timestamp]]] = {
    inFutureTransaction {
      from(clones)( c =>
        where( c.state === CloneState.cloned )
        groupBy( c.forCultistId )
        compute( max(c.attempted) )
      ).map( group =>
        (group.key, group.measures)
      ).toList.toMap
    }
  }
}

object CloneState extends Enumeration {
  type CloneState = Value
  val awaiting = Value("awaiting")
  val cloning = Value("cloning")
  val cloned = Value("cloned")
}
