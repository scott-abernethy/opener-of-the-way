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

import Mythos._
import java.sql.Timestamp
import gate.T
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import play.api.Play
import play.api.Play.current
import util.Size

class Presence extends MythosObject {
  var artifactId: Long = 0
  var state: PresenceState.Value = PresenceState.called
  var attempts: Long = 0
  var requested: Timestamp = T.now
  var attempted: Timestamp = T.yesterday
  var duration: Long = -2

  def artifact: Option[Artifact] = artifactToPresences.right(this).headOption

  // TODO refactor local path stuff in bash scripts.
  def localPath: String = "/tmp/presences/" + artifactId

  override def toString = "Presence[a" + artifactId + " =" + state + "]"
}

object Presence {
  def create(artifactId: Long, state: PresenceState.Value = PresenceState.called): Presence = {
    val x = new Presence
    x.artifactId = artifactId
    x.state = state
    x
  }

  def forArtifact(artifactId: Long): Query[Presence] = {
    from(Mythos.presences)(p =>
      where( p.artifactId === artifactId )
      select( p )
    )
  }

  def withArtifact(artifactId: Long): Query[(Artifact,Option[Presence])] = {
    join(Mythos.artifacts, Mythos.presences.leftOuter)( (a, p) =>
      where( a.id === artifactId )
      select( (a, p) )
      on(a.id === p.map(_.artifactId))
    )
  }

  lazy val maxPresenceLength = Size.megs(Play.configuration.getLong("presence.storage").getOrElse(1024L))

  def unapply(ref: Presence): Option[(Long, PresenceState.Value, Long, Timestamp)] = {
    Some( (ref.artifactId, ref.state, ref.attempts, ref.attempted) )
  }
}

object PresenceState extends Enumeration {
  type PresenceState = Value

  val called = Value("Called")
  val presenting = Value("Presenting")
  val present = Value("Present")
  val unknown = Value("Unknown")
  val released = Value("Released")
}
