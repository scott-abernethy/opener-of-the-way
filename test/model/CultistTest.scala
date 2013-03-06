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
