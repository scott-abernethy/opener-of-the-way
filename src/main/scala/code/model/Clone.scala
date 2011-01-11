package code.model

import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{EnumField, LongField, LongTypedField, DateTimeField, StringField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._

trait CloneState extends Enumeration {
  type CloneState = Value
  val waiting = Value("waiting")
  val progressing = Value("progressing")
  val done = Value("done")
}
object CloneState extends CloneState

class Clone private () extends Record[Clone] with KeyedRecord[Long] {
  def meta = Clone

  @Column(name="id")
  val idField = new LongField(this, 0)
  val artifactId = new LongField(this, 0)
  val forCultistId = new LongField(this, 0)
  val state = new EnumField[Clone,CloneState](this, CloneState)
}

object Clone extends Clone with MetaRecord[Clone] {
}
