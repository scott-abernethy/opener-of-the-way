package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import code.gate.T

class Clone extends MythosObject {
  var artifactId: Long = 0
  var forCultistId: Long = 0
  var state: CloneState.Value = CloneState.queued
  var attempts: Long = 0
  var requested: Timestamp = T.now
  var attempted: Timestamp = T.yesterday
  var duration: Long = -2
  
  def artifact: Option[Artifact] = artifactToClones.right(this).headOption
  def forCultist: Option[Cultist] = cultistToClones.right(this).headOption
}

object Clone {
  def create(artifactId: Long, forCultistId: Long, state: CloneState.Value) = {
    val c = new Clone
    c.artifactId = artifactId
    c.forCultistId = forCultistId
    c.state = state
    c
  }
}

object CloneState extends Enumeration {
  type CloneState = Value
  val queued = Value("queued")
  val progressing = Value("progressing")
  val done = Value("done")
}
