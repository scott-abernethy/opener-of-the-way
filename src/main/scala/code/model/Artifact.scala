package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, DateTimeField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._

trait ArtifactState extends Enumeration {
  type ArtifactState = Value
  val mine = Value("mine")
  val available = Value("available")
  val waiting = Value("waiting")
  val progressing = Value("progressing")
  val done = Value("done")
  val failed = Value("failed")
}
object ArtifactState extends ArtifactState

class Artifact private () extends Record[Artifact] with KeyedRecord[Long] {
  def meta = Artifact

  @Column(name="id")
  val idField = new LongField(this, 0)
  val gatewayId = new LongField(this, 0)
  val path = new StringField(this, 200, "")
  val discovered = new DateTimeField(this)
  val witnessed = new DateTimeField(this)
  def description = path.is

  lazy val gateway: ManyToOne[Gateway] = gatewayToArtifacts.right(this)
  def available = true
  def owner: Option[Cultist] = gateway.headOption.flatMap(_.cultist.headOption)
  def stateFor(cultist: Cultist): Option[ArtifactState.Value] = {
    owner match {
    case Some(c) if (c == cultist) => Some(ArtifactState.mine)
    case Some(c) => 
      from(clones)(x => where(x.artifactId.is === id and x.forCultistId === cultist.id) select(x)).headOption.map(_.state.is) match {
        case None => Some(ArtifactState.available)
        case Some(CloneState.waiting) => Some(ArtifactState.waiting)
        case Some(CloneState.progressing) => Some(ArtifactState.progressing)
        case Some(CloneState.done) => Some(ArtifactState.done)
        case _ => None
      }
    case _ => None
  }}
}

object Artifact extends Artifact with MetaRecord[Artifact] {
  def all: List[Artifact] = from(artifacts)(x => select(x)) toList
}
