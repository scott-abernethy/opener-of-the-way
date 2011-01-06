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
import scala.actors.Actor
import scala.actors.Actor._
import org.specs.mock.Mockito
import org.mockito.Matchers._

import net.liftweb.squerylrecord.SquerylRecord
import org.squeryl.adapters.H2Adapter

import code.model.Gateway

class ThresholdTestSpecsAsTest extends JUnit4(ThresholdTestSpecs)
object ThresholdTestSpecsRunner extends ConsoleRunner(ThresholdTestSpecs)

object ThresholdTestSpecs extends Specification with Mockito {
  "Threshold" should {
    val processor = mock[Processor]
    val g = Gateway.createRecord.location("10.16.15.43/public").path("frog/sheep/cow").password("cowsaregreen")
    val x = new Threshold(g, self, processor).start
    "open a gateway" in {
      processor.waitFor("threshold" :: "open" :: "10.16.15.43/public" :: "frog/sheep/cow" :: "cowsaregreen" :: Nil) returns((true, ">>> '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayFound(g2, lp) => 
          g2.location.is must be("10.16.15.43/public")
          lp must beMatching("/var/cache/foo")
        case _ => fail
      }
    }
    "close a gateway" in {
      processor.waitFor("threshold" :: "close" :: "10.16.15.43/public" :: "frog/sheep/cow" :: Nil) returns((true, Nil))
      x ! Close()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if process errrors" in {
      processor.waitFor(any[List[String]]) returns((false, ">>> '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if local path not detected" in {
      processor.waitFor(any[List[String]]) returns((true, "$$$ /var/cache/foo" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }

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
