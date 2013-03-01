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
}

object PresenceState extends Enumeration {
  type PresenceState = Value

  val called = Value("Called")
  val presenting = Value("Presenting")
  val present = Value("Present")
  val unknown = Value("Unknown")
  val released = Value("Released")
}
