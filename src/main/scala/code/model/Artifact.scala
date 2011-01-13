package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import code.model.Mythos._
import code.gate.{Wake, Warn}
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp

class Artifact(
  var gatewayId: Long, 
  var path: String, 
  var discovered: Timestamp, 
  var witnessed: Timestamp
) extends MythosObject {
  def this() = this(0, "", new Timestamp(new java.util.Date().getTime), new Timestamp(new java.util.Date().getTime))
  def description = path

  lazy val gateway: ManyToOne[Gateway] = gatewayToArtifacts.right(this)
  def available = true
  def owner: Option[Cultist] = gateway.headOption.flatMap(_.cultist.headOption)
  def stateFor(cultist: Cultist): Option[ArtifactState.Value] = owner match {
    case Some(c) if (c == cultist) => Some(ArtifactState.mine)
    case Some(c) => 
      from(clones)(x => where(x.artifactId === id and x.forCultistId === cultist.id) select(x)).headOption.map(_.state) match {
        case None => Some(ArtifactState.available)
        case Some(CloneState.waiting) => Some(ArtifactState.waiting)
        case Some(CloneState.progressing) => Some(ArtifactState.progressing)
        case Some(CloneState.done) => Some(ArtifactState.done)
        case _ => None
      }
    case _ => None
  }
  def clone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(ArtifactState.available) => 
        val clone = new Clone(id, cultist.id, CloneState.waiting)
        clones.insert(clone)
        Environment.manipulator ! Wake
        true
      case _ => false
    }
  }
  def cancelClone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(s) if (s == ArtifactState.waiting || s == ArtifactState.progressing) => 
        clones.delete(clones.where(c => c.artifactId === id and c.forCultistId === cultist.id))
        Environment.manipulator ! Warn
        true
      case _ => false
    }
  }
}

object Artifact {
  def at(id: Long): Option[Artifact] = artifacts.lookup(id)
  def all: List[Artifact] = from(artifacts)(x => select(x)) toList
  lazy val viableSources: Query[Artifact] = from(artifacts, gateways)((a, g) =>
    where(a.gatewayId === g.id and g.state === GateState.open)
    select(a)
  )
}

object ArtifactState extends Enumeration {
  type ArtifactState = Value
  val mine = Value("mine")
  val available = Value("available") // flag
  val waiting = Value("waiting") // hourglass
  val progressing = Value("progressing") // cog
  val done = Value("done") // flag green
  val failed = Value("failed") // delete | error | exclamation
}
