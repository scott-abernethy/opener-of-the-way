package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.io.File
import code.gate.T
import collection.immutable.TreeMap

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
  def owner: Option[Cultist] = inTransaction(gateway.headOption.flatMap(_.cultist.headOption))
  def stateFor(cultistId: Long, ownerId: Long, clone: Option[Clone], now: Timestamp) = {
    if (ownerId == cultistId) {
      ArtifactState.mine
    } else {
      clone.map(_.state) match {
        case None => ArtifactState.available
        case Some(CloneState.queued) => missingOr(now)(ArtifactState.queued)
        case Some(CloneState.progressing) => missingOr(now)(ArtifactState.progressing)
        case Some(CloneState.done) => ArtifactState.done
        case _ => ArtifactState.failed
      }
    }
  }
  def stateFor(cultist: Cultist): Option[ArtifactState.Value] = {
    val cultistsClone = inTransaction(from(clones)(x => where(x.artifactId === id and x.forCultistId === cultist.id) select(x)).headOption)
    owner.map(_.id) match {
      case Some(ownerId) => Some(stateFor(cultist.id, ownerId, cultistsClone, T.now))
      case _ => None
    }
  }
  private def missingOr(now: Timestamp)(state: ArtifactState.Value): ArtifactState.Value = {
    if (witnessed.before(T.agoFrom(now, 4 * 24 * 60 * 60 * 1000))) {
      ArtifactState.missing
    } else {
      state
    }
  }
  def clone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(ArtifactState.available) => 
        val clone = Clone.create(id, cultist.id, CloneState.queued)
        inTransaction(clones.insert(clone))
        true
      case _ => false
    }
  }
  def cancelClone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(s) if (s == ArtifactState.queued || s == ArtifactState.progressing || s == ArtifactState.missing) =>
        inTransaction(clones.delete(clones.where(c => c.artifactId === id and c.forCultistId === cultist.id)))
        true
      case _ => false
    }
  }
}

object Artifact {
  def find(id: Long): Option[Artifact] = inTransaction(artifacts.lookup(id))
  def all: List[Artifact] = inTransaction(from(artifacts)(x => select(x) orderBy(x.discovered desc, x.path desc)).toList)
  lazy val viableSources: Query[Artifact] = from(artifacts, gateways)((a, g) =>
    where(a.gatewayId === g.id and g.state === GateState.open)
    select(a)
  )
}

object ArtifactState extends Enumeration {
  type ArtifactState = Value
  val mine = Value("mine") //
  val available = Value("available") // not mine, not cloned
  val missing = Value("missing") // not witnessed recently
  val queued = Value("queued") // waiting on resources
  val progressing = Value("progressing") // currently cloning to sink
  val done = Value("done") // previously cloned successfully
  val failed = Value("failed") // delete | error | exclamation
}

/*
States:
- "available" = discovered, others
- "lost" = discovered, others, not recently witnessed
- "mine" = discovered, mine, available
- "removed" = discovered, mine, not recently witnessed (do i care my files are now missing?)
- "queued" = discovered, others, clone queued
- "progressing" = discovered, others, clone progressing
- "impossible" = discovered, others, not recently witnessed, clone queued
- "done" = discovered, others, clone done
 */