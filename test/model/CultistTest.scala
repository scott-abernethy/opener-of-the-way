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

import org.specs2.mutable._
import org.specs2.mock.Mockito
import model.Mythos._
import gate.T
import db.TestDb
import test.WithTestApplication
import util.PasswordHash
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.Failure

class CultistTest extends Specification with Mockito {

  "Cultist" should {

    "detect correct approach" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        cultists.insert(x)
      }
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachSuccess(c.id))
    }
    
    "detect correct approach using yuck clear text password" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes" // yuck
        cultists.insert(x)
      }
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachSuccess(c.id))
    }

    "reject approach with incorrect email or password" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        cultists.insert(x)
      }
      Cultist.approach("qux@bar.com", "grapes") must be_==(ApproachRejected)
      Cultist.approach("foo@bar.com", "rabbit") must be_==(ApproachRejected)
    }

    "reject approach with incorrect password, on expired account" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        x.expired = true
        cultists.insert(x)
      }
      Cultist.approach("foo@bar.com", "fruit") must be_==(ApproachRejected)
    }

    "reject bad approach" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        x.expired = true
        cultists.insert(x)
      }
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachExpired(c.id))
    }
    
    "change password, and not store password as clear text" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        cultists.insert(x)
      }
      val f = Cultist.changePassword("foo@bar.com", "grapes", "wrath123", "wrath123")
      Await.result(f, Duration.apply("10 seconds"))
          
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachRejected)
      Cultist.approach("foo@bar.com", "wrath123") must be_==(ApproachSuccess(c.id))
      
      val password = transaction {
        from(cultists)(x => where(x.id === c.id) select(x.password)).headOption
      }
      password.filterNot(_ == "wrath123") must beSome
    }
    
    "change password, fails when errors" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = PasswordHash.generate("grapes", Cultist.appSecret)
        cultists.insert(x)
      }
      
      val f = Cultist.changePassword("foo@bar.com", "grapes", "wrath123", "wrath12z")
      Try(Await.result(f, Duration.apply("10 seconds"))) match {
        case Failure(ex: IllegalArgumentException) if (ex.getMessage() == "New passwords do not match!") => // expected
        case other => failure("Failed, got " + other)
      }
      Cultist.approach("foo@bar.com", "wrath123") must be_==(ApproachRejected)
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachSuccess(c.id))
      
      val f2 = Cultist.changePassword("foo@bar.com", "grapes", "wrath12", "wrath12")
      Try(Await.result(f2, Duration.apply("10 seconds"))) match {
        case Failure(ex: IllegalArgumentException) if (ex.getMessage() == "New password is not acceptable - must be 8 characters!") => // expected
        case other => failure("Failed, got " + other)
      }
      Cultist.approach("foo@bar.com", "wrath12") must be_==(ApproachRejected)
      Cultist.approach("foo@bar.com", "grapes") must be_==(ApproachSuccess(c.id))
    }

  }
}
