package code.gate

import org.specs._
import scala.actors.Actor._
import org.specs.mock.Mockito
import code.model.{GateState, GateMode, Gateway}

object ThresholdTestSpecs extends Specification with Mockito {
  "Threshold" should {
    val processor = mock[Processor]
    val processing = mock[Processing]
    val g = new Gateway(0,"10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.sink, GateState.open, T.yesterday)
    val x = new Threshold(g, self, processor).start
    "open a gateway" in {
      processor.process("threshold" :: "open" :: "10.16.15.43/public" :: "frog/sheep/cow" :: "cowsaregreen" :: Nil) returns(processing)
      processing.waitFor returns(Result(true, "Gate opened '/var/cache/foo'" :: Nil, -1))
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
      processing.waitFor returns(Result(true, Nil, -1))
      x ! Close()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if process errrors" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns(Result(false, "Gate opened '/var/cache/foo'" :: Nil, -1))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if local path not detected" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns(Result(true, "Moo cow /var/cache/foo" :: Nil, -1))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location must be("10.16.15.43/public")
        case _ => fail
      }
    }
  }
}
