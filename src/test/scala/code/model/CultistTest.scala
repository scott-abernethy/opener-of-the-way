package code.model

import org.specs._
import org.specs.mock.Mockito
import code.TestDb
import code.model.Mythos._

import org.squeryl.PrimitiveTypeMode._
import code.gate.T

object CultistTest extends Specification with Mockito {

  val db = new TestDb
  db.init

  "Cultist" should {

    "detect correct approach" >> {
      db.clear
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        cultists.insert(x)
      }
      c.approach("grapes") must be_==(ApproachSuccess)
    }

    "reject approach with incorrect password" >> {
      db.clear
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        cultists.insert(x)
      }
      c.approach("rabbit") must be_==(ApproachRejected)
    }

    "reject approach with incorrect password, on expired account" >> {
      db.clear
      val c = transaction {
        val x = new Cultist
        x.email = "foo@bar.com"
        x.password = "grapes"
        x.expired = true
        cultists.insert(x)
      }
      c.approach("fruit") must be_==(ApproachRejected)
    }

    "reject bad approach" >> {
      db.clear
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

  doAfter {
    db.close
  }
}
