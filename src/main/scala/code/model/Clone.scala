package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._

class Clone(
  var artifactId: Long,
  var forCultistId: Long,
  var state: CloneState.Value,
  var attempts: Long
) extends MythosObject {
  def this() = this(0, 0, CloneState.waiting, 0)
  def artifact: Option[Artifact] = artifactToClones.right(this).headOption
  def forCultist: Option[Cultist] = cultistToClones.right(this).headOption
}

object Clone {
}

object CloneState extends Enumeration {
  type CloneState = Value
  val waiting = Value("waiting")
  val progressing = Value("progressing")
  val done = Value("done")
}
