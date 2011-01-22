package code.model

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.provider._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._

class Cultist(
  var email: String, 
  var password: String) extends MythosObject {

  def this() = this("", "")
  def sign: String = id.toString
  def destination: Option[Gateway] = from(gateways)(g => where(g.cultistId === id and g.mode === GateMode.rw) select(g) orderBy(g.id asc)) headOption

  lazy val gateways: OneToMany[Gateway] = cultistToGateways.left(this)
}

object Cultist {
  def find(id: Long): Option[Cultist] = cultists.lookup(id)
  val cultistCookie = "theyWhomAttendeth"
  object attending extends SessionVar[Box[Cultist]](checkForCookie)
  def attending_? = !attending.is.isEmpty
  def isAttending_?(cultist: Cultist) = attending.is.map(_ == cultist) getOrElse false
  def approach(cultist: Cultist) = attending(Full(cultist))
  def withdraw() = attending(Empty)
  def forEmail(email: String): Box[Cultist] = cultists.where(c => c.email === email) toSeq match {
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
