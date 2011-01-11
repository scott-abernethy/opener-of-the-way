package code.model

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.provider._
import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.record.field.{LongField, LongTypedField, OptionalDateTimeField, OptionalIntField, StringField, EmailField, PasswordField}
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._

class Cultist private () extends Record[Cultist] with KeyedRecord[Long] {
  def meta = Cultist

  @Column(name="id")
  val idField = new LongField(this, 0)
  val email = new EmailField(this, 48) {
    override def apply(s: String) = super.apply(s.toLowerCase)
    override def apply(b: Box[String]) = super.apply(b map { _.toLowerCase })
    //override def validations = valUnique(S.??("unique.email.address")) _ :: super.validations
  }
  val password = new PasswordField(this)

  def description = email.is

  lazy val gateways: OneToMany[Gateway] = cultistToGateways.left(this)
}

object Cultist extends Cultist with MetaRecord[Cultist] {
  val cultistCookie = "theyWhomAttendeth"
  object attending extends SessionVar[Box[Cultist]](checkForCookie)
  def attending_? = !attending.is.isEmpty
  def isAttending_?(cultist: Cultist) = attending.is.map(_ == cultist) getOrElse false
  def approach(cultist: Cultist) = attending(Full(cultist))
  def withdraw() = attending(Empty)
  def forEmail(email: String): Box[Cultist] = cultists.where(c => c.email.is === email) toSeq match {
    case x :: Nil => Full(x)
    case x :: xs => Full(x) // but this is bad
    case Nil => Empty
  }
  def saveCookie() {
    attending.is match {
      case Full(c) => S.addCookie(HTTPCookie(cultistCookie, c.id.toString).setMaxAge(3600 * 24 * 365).setPath("/"))
      case _ => S.addCookie(HTTPCookie(cultistCookie, "###").setPath("/"))
    }
  }
  def checkForCookie: Option[Cultist] = {
    S.cookieValue(cultistCookie) match {
      case Full(id) => cultists.lookup(id.toLong)
      case _ => None
    }
  }
}
