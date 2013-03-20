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

class CultistTest extends Specification with Mockito {

  "Cultist" should {

    "detect correct approach" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        cultists.insert(x)
      }
      c.approach("grapes") must be_==(ApproachSuccess)
    }

    "reject approach with incorrect password" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        cultists.insert(x)
      }
      c.approach("rabbit") must be_==(ApproachRejected)
    }

    "reject approach with incorrect password, on expired account" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        x.expired = true
        cultists.insert(x)
      }
      c.approach("fruit") must be_==(ApproachRejected)
    }

    "reject bad approach" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        x.expired = true
        cultists.insert(x)
      }
      c.approach("grapes") must be_==(ApproachExpired)
    }

  }
}
