package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.io.File
import code.gate.T
import collection.immutable.TreeMap
import xml.Node

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

  def stateFor(cultistId: Long, ownerId: Long, clone: Option[Clone], now: Timestamp): Option[ArtifactState.Value] = {
    if (ownerId == cultistId) {
      missingOr(now)(ArtifactState.proffered, ArtifactState.profferedLost)
    } else {
      clone.map(_.state) match {
        case None => missingOr(now)(ArtifactState.glimpsed, ArtifactState.lost)
        case Some(CloneState.awaiting) => missingOr(now)(ArtifactState.awaiting, ArtifactState.awaitingLost)
        case Some(CloneState.cloning) => Some(ArtifactState.cloning)
        case Some(CloneState.cloned) => Some(ArtifactState.cloned)
        case _ => None
      }
    }
  }

  def stateFor(cultist: Cultist): Option[ArtifactState.Value] = {
    val cultistsClone = inTransaction(from(clones)(x => where(x.artifactId === id and x.forCultistId === cultist.id) select(x)).headOption)
    owner.map(_.id) match {
      case Some(ownerId) => stateFor(cultist.id, ownerId, cultistsClone, T.now)
      case _ => None
    }
  }

  private def missingOr(now: Timestamp)(state: ArtifactState.Value, missing: ArtifactState.Value): Option[ArtifactState.Value] = {
    if (witnessed.before(T.agoFrom(now, Artifact.lostAfter))) {
      Some(missing)
    } else {
      Some(state)
    }
  }

  def clone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(s) if ArtifactState.possible_?(s) =>
        val clone = Clone.create(id, cultist.id, CloneState.awaiting)
        inTransaction(clones.insert(clone))
        true
      case _ => false
    }
  }

  def cancelClone(cultist: Cultist): Boolean = {
    stateFor(cultist) match {
      case Some(s) if ArtifactState.awaiting_?(s) =>
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

  lazy val immatureBefore = 10 * 60 * 1000L

  lazy val lostAfter = 4 * 24 * 60 * 60 * 1000L

  lazy val goneAfter = 12 * 24 * 60 * 60 * 1000L
}

object ArtifactState extends Enumeration {
  type ArtifactState = Value
  val proffered = Value("proffered")
  val profferedLost = Value("proffered, lost")
  val glimpsed = Value("glimpsed")
  val lost = Value("lost")
  val awaiting = Value("awaiting")
  val awaitingLost = Value("awaiting, lost")
  val cloning = Value("cloning")
  val cloned = Value("cloned")

  def symbol(s: ArtifactState.Value): Option[Node] = s match {
    case ArtifactState.proffered => Some(<img src="/static/c_proffered.png" title="Proffered"/>)
    case ArtifactState.profferedLost => Some(<img src="/static/c_proffered_lost.png" title="Proffered, lost"/>)
    case ArtifactState.awaiting => Some(<img src="/static/c_awaiting.png" title="Awaiting"/>)
    case ArtifactState.awaitingLost => Some(<img src="/static/c_awaiting_lost.png" title="Awaiting, lost"/>)
    case ArtifactState.glimpsed => Some(<img src="/static/c_glimpsed.png" title="Glimpsed"/>)
    case ArtifactState.lost => Some(<img src="/static/c_glimpsed_lost.png" title="Glimpsed, lost"/>)
    case ArtifactState.cloning => Some(<img src="/static/c_cloning.gif" title="Cloning..."/>)
    case ArtifactState.cloned => Some(<img src="/static/c_cloned.png" title="Cloned"/>)
    case _ => None
  }

  val unknownSymbol: Node = <img src="/static/c_unknown.png" title="?"/>
  
  def awaiting_?(s: ArtifactState.Value): Boolean = s == ArtifactState.awaiting || s ==  ArtifactState.awaitingLost || s == ArtifactState.cloning

  def possible_?(s: ArtifactState.Value): Boolean = s == ArtifactState.glimpsed || s == ArtifactState.lost

  def proffered_?(s: ArtifactState.Value): Boolean = s == ArtifactState.proffered || s == ArtifactState.profferedLost
}

/*
await
lost
linger
wait
disposed
erased
glimpsed
at hand
obtainable
near

States:
- "available" = discovered, others
- "lost" = discovered, others, not recently witnessed
- "mine" = discovered, mine, available
- "removed" = discovered, mine, not recently witnessed (do i care my files are now missing?)
- "queued" = discovered, others, clone queued
- "progressing" = discovered, others, clone progressing
- "queued, lost" = discovered, others, not recently witnessed, clone queued (missing overrides the clone)
- "done" = discovered, others, clone done
 */