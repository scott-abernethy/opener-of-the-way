/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.util.Properties
import java.io.FileReader
import gate.{Millis, T}
import java.sql.Timestamp
import xml.Node
import scala.util.matching.Regex
import play.api.libs.json.Json
import util.DatePresentation
import concurrent.Future
import util.FutureTransaction._
import util.PasswordHash
import play.api.Play

sealed abstract class ApproachResult
case class ApproachSuccess(cid: Long) extends ApproachResult
case object ApproachRejected extends ApproachResult
case class ApproachExpired(cid: Long) extends ApproachResult

class Cultist extends MythosObject {
  var email: String = ""
  var password: String = ""
  var insane: Boolean = false
  var expired: Boolean = false
  var locked: Boolean = false
  var recruitedBy: Long = -1
  var seen: Option[Timestamp] = None
  var shut: Option[Timestamp] = None

  def destination: Option[Gateway] = from(gateways)(g => where(g.cultistId === id and g.sink === true) select(g) orderBy(g.id.asc)).headOption

  lazy val gateways: OneToMany[Gateway] = cultistToGateways.left(this)

  lazy val activeClones: Query[Clone] = from(clones)(c =>
    where(c.forCultistId === id and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
    select(c)
  )

  def toJson = {
    Json.obj(
      "id" -> id,
      "shut" -> shut.isDefined,
      "shutUntil" -> (shut.map(t => DatePresentation.atAbbreviation(t.getTime)).getOrElse("-").toString)
    )
  }
}

object Cultist {
  
  import play.api.Play.current
  val appSecret = Play.configuration.getString("application.secret").getOrElse("g7s6d8")

  def create(email: String, password: String): Cultist = {
    val x = new Cultist
    x.email = email
    x.password = PasswordHash.generate(password, appSecret)
    x
  }

  def insertRecruit(email: String, password: String, recruiter: Long): Option[Cultist] = {
    val c = create(email, password)
    c.recruitedBy = recruiter
    c.expired = true // forces password change
    c.locked = true // TODO currently has no impact - plan was it would require unlocking by the insane before they can glimpse the truth
    transaction {
      val free = cultists.where(_.email === c.email).isEmpty
      if (free) {
        Some(cultists.insert(c))
      }
      else {
        None
      }
    }
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

  def forEmail(email: String): Option[Cultist] = {
    inTransaction(cultists.where(c => c.email === email).toList) match {
      case x :: Nil => Some(x)
      case x :: xs => Some(x) // but this is bad
      case Nil => None
    }
  }

  def insane_?(id: Long): Boolean = {
    inTransaction(
      from(cultists)(c =>
        where(c.id === id and c.insane === true)
        select(c.insane)
      ).headOption.isDefined
    )
  }

  lazy val unlockAfter = Millis.days(1);

  def all(): Future[List[(Cultist, String)]] = {
    inFutureTransaction {
      join(cultists, pseudonyms)( (w, n) =>
        select( (w, n.name) )
        orderBy( n.name.asc )
        on( w.id === n.id )
      ).toList
    }
  }
  
  def approach(email: String, password: String): ApproachResult = {
    transaction {
      val cultist = from(cultists)(c => where(c.email === email) select(c)).headOption
      val authorized = cultist.filter(c => checkPassword(c.password, password))
    
      authorized match {
        case None => ApproachRejected
        case Some(x) if (x.expired) => ApproachExpired(x.id)
        case Some(x) => {
          update(cultists)(c =>
            where(c.id === x.id)
            set(c.seen := Some(T.now))
          )
          ApproachSuccess(x.id)
        }
      }
    }
  }
  
  def changePassword(email: String, oldPassword: String, newPassword: String, repeatNewPassword: String): Future[Unit] = {
    futureTransaction ({
      val cultistOption = from(cultists)(c => where(c.email === email) select(c)).headOption
      cultistOption match {
        case Some(cultist) if checkPassword(cultist.password, oldPassword) => {
          if (newPassword != repeatNewPassword) throw new IllegalArgumentException("New passwords do not match!") 
          else 
            if (newPassword.length < 8) throw new IllegalArgumentException("New password is not acceptable - must be 8 characters!")
            else savePassword(newPassword, cultist.id)
        }
        case _ => {
          throw new IllegalArgumentException("Email and/or password are incorrect!")
        }
      }
    })
  }
  
  private def checkPassword(storedPassword: String, input: String): Boolean = {
    def hashCheck: Boolean = {
      PasswordHash.check(input, storedPassword, appSecret)
    }
    def clearCheck: Boolean = {
      // Temporary. Because passwords were originally stored in clear text (ewww)
      input == storedPassword
    }
    hashCheck || clearCheck
  }
  
  private def savePassword(newPassword: String, cid: Long): Unit = {
    val newHash = PasswordHash.generate(newPassword, appSecret)
    cultists.update(x =>
      where(x.id === cid)
      set(x.password := newHash, x.expired := false)
    )
  }
}
