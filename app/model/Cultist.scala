package model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.util.Properties
import java.io.FileReader
import gate.T
import java.sql.Timestamp
import xml.Node
import scala.util.matching.Regex
import play.api.libs.json.Json
import util.DatePresentation

sealed abstract class ApproachResult
case object ApproachSuccess extends ApproachResult
case object ApproachRejected extends ApproachResult
case object ApproachExpired extends ApproachResult

class Cultist extends MythosObject {
  var email: String = ""
  var password: String = ""
  var insane: Boolean = false
  var expired: Boolean = false
  var locked: Boolean = false
  var recruitedBy: Long = -1
  var seen: Option[Timestamp] = None
  var shut: Option[Timestamp] = None

  def destination: Option[Gateway] = from(gateways)(g => where(g.cultistId === id and g.sink === true) select(g) orderBy(g.id asc)).headOption

  lazy val gateways: OneToMany[Gateway] = cultistToGateways.left(this)

  lazy val activeClones: Query[Clone] = from(clones)(c =>
    where(c.forCultistId === id and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
    select(c)
  )

  def approach(submittedPassword: String): ApproachResult = {
    (password == submittedPassword, expired) match {
      case (false, _) => ApproachRejected
      case (_, true) => ApproachExpired
      case _ => {
        transaction {
          update(cultists)(c =>
            where(c.id === id)
            set(c.seen := Some(T.now))
          )
        }
        ApproachSuccess
      }
    }
  }

  def toJson = {
    Json.obj(
      "id" -> id,
      "shut" -> shut.isDefined,
      "shutUntil" -> (shut.map(t => DatePresentation.atAbbreviation(t.getTime)).getOrElse("-").toString)
    )
  }
}

object Cultist {
  def create(email: String, password: String): Cultist = {
    val x = new Cultist
    x.email = email
    x.password = password
    x
  }

  val ValidEmail: Regex = """([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,4})""".r

  def find(id: String): Option[Cultist] = {
    try {
      find(id.toLong)
    } catch {
      case _: NumberFormatException => None
    }
  }
  def find(id: Long): Option[Cultist] = cultists.lookup(id)
  val cultistCookie = "theyWhomAttendethIt"

  def withdraw() {
    // TODO
    //attending(Empty)
    //S.session.foreach(_.destroySession())
  }

  def forEmail(email: String): Option[Cultist] = {
    inTransaction(cultists.where(c => c.email === email).toList) match {
      case x :: Nil => Some(x)
      case x :: xs => Some(x) // but this is bad
      case Nil => None
    }
  }

  def saveCookie() {
    // TODO
//    val text = attending.is.map(_.id.toString).openOr("###")
//    S.addCookie(HTTPCookie(cultistCookie, text).setMaxAge(3600 * 24 * 7).setPath("/"))
  }

  def checkForCookie: Option[Cultist] = {
    // TODO
//    S.cookieValue(cultistCookie) match {
//      case Full(id) => try { find(id.toLong) } catch { case _ => None }
//      case _ => None
//    }
    None
  }

//  def signFor(cultist: Option[Cultist]): String =
//  {
//    cultist.map(_.sign).filter(x => x != null && x.size > 0).getOrElse("???")
//  }
//
//  def sigalFor(cultist: Option[Cultist]): Node =
//  {
//    <span class="sigal">{ signFor(cultist) }</span>
//  }

  lazy val unlockAfter = 4 * 60 * 60 * 1000L
}
