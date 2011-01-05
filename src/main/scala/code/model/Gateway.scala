package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, OptionalDateTimeField, OptionalIntField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column

class Gateway private () extends Record[Gateway] with KeyedRecord[Long] {
  def meta = Gateway

  @Column(name="id")
  val idField = new LongField(this, 0)
  val cultistId = new LongField(this, 0)
  val uri = new StringField(this, 100, "")

  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToGateways.right(this)
  lazy val artifacts: OneToMany[Artifact] = Mythos.gatewayToArtifacts.left(this)
}

object Gateway extends Gateway with MetaRecord[Gateway]
