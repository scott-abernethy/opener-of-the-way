package code.gate

import org.specs._
import org.specs.runner.JUnit4
import org.specs.runner.ConsoleRunner
import net.liftweb._
import http._
import net.liftweb.util._
import net.liftweb.common._
import org.specs.matcher._
import org.specs.specification._
import Helpers._
import mapper._

import net.liftweb.squerylrecord.SquerylRecord
import org.squeryl.adapters.H2Adapter

import code.model.Gateway

class ThresholdTestSpecsAsTest extends JUnit4(ThresholdTestSpecs)
object ThresholdTestSpecsRunner extends ConsoleRunner(ThresholdTestSpecs)

object ThresholdTestSpecs extends Specification {
  "Threshold" should {
    "open a gateway" in {
      val g = Gateway.createRecord.location("10.16.15.43/public").path("frog/sheep/cow").password("cowsaregreen")
      val x = new Threshold(g)
      x.open must be(true)
      /*
      import net.liftweb.squerylrecord.RecordTypeMode._
      val vendor = new StandardDBVendor("org.h2.Driver", "jdbc:h2:mem:test", Empty, Empty)
      try {
        DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
        SquerylRecord.init(() => new H2Adapter)

        DB.use(DefaultConnectionIdentifier) { _ => 
          Mythos.create  

        }
      } finally {
        vendor.closeAllConnections_!
      }
      */
    }
  }
}
