package code.model

import Mythos._
import java.sql.Timestamp
import code.gate.T
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._

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

  // Set this in properties file? YES AS THEN DON'T NEED TO CHANGE TEST EVERY TIME.
  lazy val gigaByteLength = 1024L * 1024 * 1024;
  lazy val maxPresenceLength = gigaByteLength * 320
}

object PresenceState extends Enumeration {
  type PresenceState = Value

  val called = Value("Called")
  val presenting = Value("Presenting")
  val present = Value("Present")
  val unknown = Value("Unknown")
  val released = Value("Released")
}
