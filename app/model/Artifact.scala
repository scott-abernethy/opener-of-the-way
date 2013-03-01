package model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.io.File
import gate.{Millis, T}
import collection.immutable.TreeMap
import xml.Node
import play.api.libs.json.{JsObject, Json, JsValue}
import util.{Size, FileUtil}
import util.FutureTransaction._
import concurrent.Future


class Artifact extends MythosObject {
  var gatewayId: Long = 0
  var path: String = ""
  var discovered: Timestamp = T.now
  var witnessed: Timestamp = T.now
  var length: Long = -1

  def description = if (path.isEmpty) "-" else path.substring(1) // Path is currently prefixed with '/'

  lazy val gateway: ManyToOne[Gateway] = gatewayToArtifacts.right(this)

  def localPath: Option[String] = gateway.headOption.map(g => new File(g.localPath, path).getPath)

  def fileName: String = new File(path).getName
  
  def available = true

  def owner: Option[Cultist] = inTransaction(gateway.headOption.flatMap(_.cultist.headOption))

  def stateFor(cultistId: Long, ownerId: Long, clone: Option[Clone], now: Timestamp, presence: Option[Presence]): Option[ArtifactState.Value] = {
    if (ownerId == cultistId) {
      presentLostOrOther(presence, now)(ArtifactState.profferedPresent, ArtifactState.profferedLost, ArtifactState.proffered)
    } else {
      clone.map(_.state) match {
        case None => presentLostOrOther(presence, now)(ArtifactState.present, ArtifactState.lost, ArtifactState.glimpsed)
        case Some(CloneState.awaiting) => presentLostOrOther(presence, now)(ArtifactState.awaitingPresent, ArtifactState.awaitingLost, ArtifactState.awaiting)
        case Some(CloneState.cloning) => Some(ArtifactState.cloning)
        case Some(CloneState.cloned) => Some(ArtifactState.cloned)
        case _ => None
      }
    }
  }

  def stateFor(cultistId: Long): Option[ArtifactState.Value] = {
    val (cultistsClone, present) = inTransaction {
      val cultistsClone = from(clones)(x => where(x.artifactId === id and x.forCultistId === cultistId) select(x)).headOption
      val present = from(presences)(p => where(p.artifactId === id) select(p)).headOption
      (cultistsClone, present)
    }

    owner.map(_.id) match {
      case Some(ownerId) => stateFor(cultistId, ownerId, cultistsClone, T.now, present)
      case _ => None
    }
  }

  private def presentLostOrOther(presence: Option[Presence], now: Timestamp)(present: ArtifactState.Value, missing: ArtifactState.Value, other: ArtifactState.Value): Option[ArtifactState.Value] = {
    if (presence.exists(_.state == PresenceState.present)) {
      return Some(present)
    }
    if (witnessed.before(T.agoFrom(now, Artifact.lostAfter))) {
      Some(missing)
    }
    else {
      Some(other)
    }
  }

  def clone(cultistId: Long): Boolean = {
    stateFor(cultistId) match {
      case Some(ArtifactState.cloned) =>
        inTransaction{
          clones.update(c =>
            where(
              c.artifactId === id and
              c.forCultistId === cultistId
            )
            set(
              c.state := CloneState.awaiting,
              c.requested := T.now,
              c.attempts := 0L,
              c.repeats := c.repeats.~ + 1L
            )
          )
        }
        true
      case Some(s) if ArtifactState.possible_?(s) =>
        val clone = Clone.create(id, cultistId, CloneState.awaiting)
        inTransaction(clones.insert(clone))
        true
      case _ => false
    }
  }

  def cancelClone(cultistId: Long): Boolean = {
    stateFor(cultistId) match {
      case Some(s) if ArtifactState.awaiting_?(s) =>
        inTransaction(clones.delete(clones.where(c => c.artifactId === id and c.forCultistId === cultistId)))
        true
      case _ => false
    }
  }

  def toggleClone(cultistId: Long): Option[Boolean] = {
    stateFor(cultistId) match {
      case Some(ArtifactState.cloned) =>
        inTransaction{
          clones.update(c =>
            where(
              c.artifactId === id and
              c.forCultistId === cultistId
            )
            set(
              c.state := CloneState.awaiting,
              c.requested := T.now,
              c.attempts := 0L,
              c.repeats := c.repeats.~ + 1L
            )
          )
        }
        Some(true)
      case Some(s) if ArtifactState.possible_?(s) =>
        val clone = Clone.create(id, cultistId, CloneState.awaiting)
        inTransaction(clones.insert(clone))
        Some(true)
      case Some(s) if ArtifactState.awaiting_?(s) =>
        inTransaction(clones.delete(clones.where(c => c.artifactId === id and c.forCultistId === cultistId)))
        Some(false)
      case _ => None
    }
  }

  override def toString = "Artifact[" + id + ";" + path + ";" + length + "]"

  def toJson: JsObject = {
    Json.obj(
      "id" -> id,
      "desc" -> description,
      "size" -> Size.short(length)
    )
  }
}

object Artifact {
  def find(id: Long): Option[Artifact] = inTransaction(artifacts.lookup(id))

  def all: List[Artifact] = inTransaction(from(artifacts)(x => select(x) orderBy(x.discovered desc, x.path desc)).toList)

  def findUnique(gatewayId: Long, path: String): Option[Artifact] = {
    inTransaction(
      from(artifacts)(x =>
        where(x.gatewayId === gatewayId and x.path === path)
        select(x)
      ).headOption
    )
  }

  lazy val viableSources: Query[Artifact] = from(artifacts, gateways)((a, g) =>
    where(a.gatewayId === g.id and g.state === GateState.open)
    select(a)
  )

  def create(gatewayId: Long, path: String, discovered: Timestamp, witnessed: Timestamp, length: Long = -1) = {
    val x = new Artifact
    x.gatewayId = gatewayId
    x.path = path
    x.discovered = discovered
    x.witnessed = witnessed
    x.length = length
    x
  }

  lazy val immatureBefore = 10 * 60 * 1000L

  lazy val lostAfter = 4 * 24 * 60 * 60 * 1000L

  lazy val goneAfter = 12 * 24 * 60 * 60 * 1000L

  lazy val notNewsAfter = Millis.days(60)

  def discovered(after: Timestamp): Future[List[Artifact]] = {
    inFutureTransaction (
      from(artifacts)( a =>
        where( a.discovered > after )
        select( a )
        orderBy( a.discovered desc )
      ).toList
    )
  }
}

object ArtifactState extends Enumeration {
  type ArtifactState = Value
  val proffered = Value("Proffered")
  val profferedLost = Value("Proffered, lost")
  val profferedPresent = Value("Proffered, present")
  val glimpsed = Value("Glimpsed")
  val present = Value("Present")
  val lost = Value("Lost")
  val awaiting = Value("Awaiting")
  val awaitingLost = Value("Awaiting, lost")
  val awaitingPresent = Value("Awaiting, present")
  val cloning = Value("Cloning")
  val cloned = Value("Cloned")

  def symbol(s: ArtifactState.Value): Option[Node] = s match {
    case ArtifactState.proffered => Some(<img src="/static/c_proffered.png" title="Proffered"/>)
    case ArtifactState.profferedLost => Some(<img src="/static/c_proffered_lost.png" title="Proffered, lost"/>)
    case ArtifactState.profferedPresent => Some(<img src="/static/c_proffered_present.png" title="Proffered, present"/>)
    case ArtifactState.awaiting => Some(<img src="/static/c_awaiting.png" title="Awaiting"/>)
    case ArtifactState.awaitingLost => Some(<img src="/static/c_awaiting_lost.png" title="Awaiting, lost"/>)
    case ArtifactState.awaitingPresent => Some(<img src="/static/c_awaiting_present.png" title="Awaiting, present"/>)
    case ArtifactState.glimpsed => Some(<img src="/static/c_glimpsed.png" title="Glimpsed"/>)
    case ArtifactState.lost => Some(<img src="/static/c_glimpsed_lost.png" title="Glimpsed, lost"/>)
    case ArtifactState.present => Some(<img src="/static/c_glimpsed_present.png" title="Present"/>)
    case ArtifactState.cloning => Some(<img src="/static/c_cloning.gif" title="Cloning..."/>)
    case ArtifactState.cloned => Some(<img src="/static/c_cloned.png" title="Cloned"/>)
    case _ => None
  }

  val unknownSymbol: Node = <img src="/static/c_unknown.png" title="?"/>
  
  def awaiting_?(s: ArtifactState.Value): Boolean = s == ArtifactState.awaiting || s ==  ArtifactState.awaitingLost || s == ArtifactState.awaitingPresent || s == ArtifactState.cloning

  def possible_?(s: ArtifactState.Value): Boolean = s == ArtifactState.glimpsed || s == ArtifactState.lost || s == ArtifactState.present || s == ArtifactState.cloned

  def proffered_?(s: ArtifactState.Value): Boolean = s == ArtifactState.proffered || s == ArtifactState.profferedLost || s == ArtifactState.profferedPresent

  def toJson(state: Option[ArtifactState.Value]): JsObject = {
    val (icon, desc, clazz) = info(state)
    Json.obj("icon" -> icon, "desc" -> desc, "class" -> clazz)
  }

  def info(state: Option[ArtifactState.Value]): (String, String, String) = state match {
    case Some(ArtifactState.proffered) => ("icon-gift", "Proffered", "")
    case Some(ArtifactState.profferedLost) => ("icon-gift", "Proffered, lost", "s-lost")
    case Some(ArtifactState.profferedPresent) => ("icon-gift", "Proffered, present", "")
    case Some(ArtifactState.awaiting) => ("icon-ok-sign", "Awaiting", "s-awaiting")
    case Some(ArtifactState.awaitingLost) => ("icon-ok-sign", "Awaiting, lost", "s-error-lost")
    case Some(ArtifactState.awaitingPresent) => ("icon-ok-sign", "Awaiting, present", "s-awaiting")
    case Some(ArtifactState.glimpsed) => ("icon-circle-blank", "Glimpsed", "")
    case Some(ArtifactState.lost) => ("icon-ban-circle", "Glimpsed, lost", "s-lost")
    case Some(ArtifactState.present) => ("icon-circle-blank", "Present", "")
    case Some(ArtifactState.cloning) => ("icon-cog icon-spin", "Cloning...", "s-cloning")
    case Some(ArtifactState.cloned) => ("icon-asterisk", "Cloned", "")
    case _ => ("icon-question-sign", "???", "s-error")
  }
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