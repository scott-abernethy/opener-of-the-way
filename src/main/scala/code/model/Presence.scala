package code.model

import Mythos._
import java.sql.Timestamp
import code.gate.T

class Presence extends MythosObject {
  var artifactId: Long = 0
  var state: PresenceState.Value = PresenceState.called
  var attempts: Long = 0
  var requested: Timestamp = T.now
  var attempted: Timestamp = T.yesterday
  var duration: Long = -2

  def artifact: Option[Artifact] = artifactToPresences.right(this).headOption
}

object Presence {
  def create(artifactId: Long, state: PresenceState.Value = PresenceState.called): Presence = {
    val x = new Presence
    x.artifactId = artifactId
    x.state = state
    x
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
