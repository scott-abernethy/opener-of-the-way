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
import code.model.{GateState, GateMode, Gateway}

class ThresholdTestSpecsAsTest extends JUnit4(ThresholdTestSpecs)
object ThresholdTestSpecsRunner extends ConsoleRunner(ThresholdTestSpecs)

object ThresholdTestSpecs extends Specification with Mockito {
  "Threshold" should {
    val processor = mock[Processor]
    val processing = mock[Processing]
    val g = new Gateway(0,"10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.sink, GateState.open, T.zero)
    val x = new Threshold(g, self, processor).start
    "open a gateway" in {
      processor.process("threshold" :: "open" :: "10.16.15.43/public" :: "frog/sheep/cow" :: "cowsaregreen" :: Nil) returns(processing)
      processing.waitFor returns((true, "Gate opened '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayFound(g2, lp) => 
          g2.location must be("10.16.15.43/public")
          lp must beMatching("/var/cache/foo")
        case _ => fail
      }
    }
    "close a gateway" in {
      processor.process("threshold" :: "close" :: "10.16.15.43/public" :: "frog/sheep/cow" :: Nil) returns(processing)
      processing.waitFor returns((true, Nil))
      x ! Close()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if process errrors" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns((false, "Gate opened '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if local path not detected" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns((true, "Moo cow /var/cache/foo" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
  }
}
