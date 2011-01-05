package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, OptionalDateTimeField, OptionalIntField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column

class Tome private () extends Record[Tome] with KeyedRecord[Long] {
  def meta = Tome

  @Column(name="id")
  val idField = new LongField(this, 0)
  val cultistId = new LongField(this, 0)
  val uri = new StringField(this, 100, "")

  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToTomes.right(this)
  lazy val spells: OneToMany[Spell] = Mythos.tomeToSpells.left(this)
}

object Tome extends Tome with MetaRecord[Tome]
