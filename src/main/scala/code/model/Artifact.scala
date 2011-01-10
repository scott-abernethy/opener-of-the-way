package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, DateTimeField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._

class Artifact private () extends Record[Artifact] with KeyedRecord[Long] {
  def meta = Artifact

  @Column(name="id")
  val idField = new LongField(this, 0)
  val gatewayId = new LongField(this, 0)
  val path = new StringField(this, 200, "")
  val discovered = new DateTimeField(this)
  val witnessed = new DateTimeField(this)

  lazy val gateway: ManyToOne[Gateway] = gatewayToArtifacts.right(this)
}

object Artifact extends Artifact with MetaRecord[Artifact] {
  def all: List[Artifact] = from(artifacts)(x => select(x)) toList
}
