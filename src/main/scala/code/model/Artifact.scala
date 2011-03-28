package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.io.File
import code.gate.T

class Artifact(
  var gatewayId: Long, 
  var path: String, 
  var discovered: Timestamp, 
  var witnessed: Timestamp
) extends MythosObject {
  def this() = this(0, "", T.now, T.now)
  def description = path
  lazy val gateway: ManyToOne[Gateway] = gatewayToArtifacts.right(this)
  def localPath: Option[String] = gateway.headOption.map(g => new File(g.localPath, path).getPath)
  def available = true
  def owner: Option[Cultist] = gateway.headOption.flatMap(_.cultist.headOption)
  def stateFor(cultist: Cultist): Option[ArtifactState.Value] = owner match {
    case Some(c) if (c == cultist) => Some(ArtifactState.mine)
    case Some(c) => 
      from(clones)(x => where(x.artifactId === id and x.forCultistId === cultist.id) select(x)).headOption.map(_.state) match {
        case None => Some(ArtifactState.available)
        case Some(CloneState.queued) => Some(ArtifactState.queued)
        case Some(CloneState.progressing) => Some(ArtifactState.progressing)
        case Some(CloneState.done) => Some(ArtifactState.done)
        case _ => None
      }
    case _ => None
  }
  def clone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(ArtifactState.available) => 
        val clone = new Clone(id, cultist.id, CloneState.queued, 0)
        clones.insert(clone)
        true
      case _ => false
    }
  }
  def cancelClone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(s) if (s == ArtifactState.queued || s == ArtifactState.progressing) =>
        clones.delete(clones.where(c => c.artifactId === id and c.forCultistId === cultist.id))
        true
      case _ => false
    }
  }
}

object Artifact {
  def find(id: Long): Option[Artifact] = artifacts.lookup(id)
  def all: List[Artifact] = from(artifacts)(x => select(x) orderBy(x.discovered desc, x.path desc)) toList
  lazy val viableSources: Query[Artifact] = from(artifacts, gateways)((a, g) =>
    where(a.gatewayId === g.id and g.state === GateState.open)
    select(a)
  )
}

object ArtifactState extends Enumeration {
  type ArtifactState = Value
  val mine = Value("mine") //
  val available = Value("available") // flag
  val queued = Value("queued") // hourglass
  val progressing = Value("progressing") // cog
  val done = Value("done") // flag green
  val failed = Value("failed") // delete | error | exclamation
}
