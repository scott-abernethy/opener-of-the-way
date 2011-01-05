package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, DateTimeField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column

class Spell private () extends Record[Spell] with KeyedRecord[Long] {
  def meta = Spell

  @Column(name="id")
  val idField = new LongField(this, 0)
  val tomeId = new LongField(this, 0)
  val path = new StringField(this, 200, "")
  val discovered = new DateTimeField(this)
  val witnessed = new DateTimeField(this)

  lazy val tome: ManyToOne[Tome] = Mythos.tomeToSpells.right(this)
}

object Spell extends Spell with MetaRecord[Spell]
