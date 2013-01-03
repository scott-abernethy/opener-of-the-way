package model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import gate.T

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
}

object CloneState extends Enumeration {
  type CloneState = Value
  val awaiting = Value("awaiting")
  val cloning = Value("cloning")
  val cloned = Value("cloned")
}
