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
  val location = new StringField(this, 100, "") // hostname/sharename
  val path = new StringField(this, 100, "") // folder/subfolder/tcfilename
  val password = new StringField(this, 100, "") // storing in cleartext as none should have access to db

  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToGateways.right(this)
  lazy val artifacts: OneToMany[Artifact] = Mythos.gatewayToArtifacts.left(this)
}

object Gateway extends Gateway with MetaRecord[Gateway]
