package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, OptionalDateTimeField, OptionalIntField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column

class Cultist private () extends Record[Cultist] with KeyedRecord[Long] {
  def meta = Cultist

  @Column(name="id")
  val idField = new LongField(this, 0)
  val name = new StringField(this, "")

  lazy val gateways: OneToMany[Gateway] = Mythos.cultistToGateways.left(this)
}

object Cultist extends Cultist with MetaRecord[Cultist]
