package code.model

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.provider._
import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.util.Properties
import java.io.FileReader
import net.liftweb.util.Helpers
import code.gate.T
import java.sql.Timestamp
import util.matching.Regex

class Cultist extends MythosObject {
  var email: String = ""
  var password: String = ""
  var insane: Boolean = false
  var expired: Boolean = false
  var locked: Boolean = false
  var recruitedBy: Long = -1
  var seen: Option[Timestamp] = None

  def sign: String = Cultist.loadCodename(id.toString) // lame but simple

  def destination: Option[Gateway] = from(gateways)(g => where(g.cultistId === id and g.sink === true) select(g) orderBy(g.id asc)) headOption

  lazy val gateways: OneToMany[Gateway] = cultistToGateways.left(this)

  lazy val activeClones: Query[Clone] = from(clones)(c =>
    where(c.forCultistId === id and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
    select(c)
  )
}

object Cultist {
  def create(email: String, password: String): Cultist = {
    val x = new Cultist
    x.email = email
    x.password = password
    x
  }

  val ValidEmail: Regex = """([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,4})""".r

  def find(id: Long): Option[Cultist] = cultists.lookup(id)
  val cultistCookie = "theyWhomAttendethIt"
  lazy val codenames: Properties = {
    val p = new Properties
    p.load(Cultist.getClass.getClassLoader.getResourceAsStream("props/codename.props"))
    p
  }
  object attending extends SessionVar[Box[Cultist]](checkForCookie)
  def attending_? = !attending.is.isEmpty
  def isAttending_?(cultist: Cultist) = attending.is.map(_ == cultist) getOrElse false

  def approach(cultist: Cultist, password: String): Option[Cultist] = {
    if (cultist.password == password) {
      attending(Full(cultist))
      Some(cultist)
    } else {
      None
    }
  }

  def withdraw() {
    attending(Empty)
    S.session.foreach(_.destroySession())
  }

  def forEmail(email: String): Box[Cultist] = {
    inTransaction(cultists.where(c => c.email === email).toList) match {
      case x :: Nil => Full(x)
      case x :: xs => Full(x) // but this is bad
      case Nil => Empty
    }
  }

  def saveCookie() {
    val text = attending.is.map(_.id.toString).openOr("###")
    S.addCookie(HTTPCookie(cultistCookie, text).setMaxAge(3600 * 24 * 7).setPath("/"))
  }
  def checkForCookie: Option[Cultist] = {
    S.cookieValue(cultistCookie) match {
      case Full(id) => try { find(id.toLong) } catch { case _ => None }
      case _ => None
    }
  }
  def loadCodename(index: String): String = codenames.getProperty(index, index + "?")
}
