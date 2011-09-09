package code.gate

import org.specs._
import scala.actors.Actor._
import org.specs.mock.Mockito
import akka.testkit.{TestKit, TestActorRef}
import org.squeryl.PrimitiveTypeMode._
import code.TestDb
import code.model.{Mythos, GateState, GateMode, Gateway}
import akka.util.duration._

object ThresholdTest extends Specification with Mockito with TestKit {

  val db = new TestDb

  doBeforeSpec {
    db.init
    db.reset
  }

  "Threshold" should {
    val processor = mock[Processor]
    val processing = mock[Processing]
    val g = new Gateway
    g.location = "10.16.15.43/public"
    g.path = "frog/sheep/cow"
    g.password = "cowsaregreen"
    g.mode = GateMode.sink
    g.state = GateState.open

    "open a gateway" >> {
      processor.process("threshold" :: "open" :: "10.16.15.43/public" :: "frog/sheep/cow" :: "cowsaregreen" :: Nil) returns(processing)
      processing.waitFor returns(Result(true, "Gate opened '/var/cache/foo'" :: Nil, -1))
      val x = TestActorRef(new Threshold(processor)).start
      x ! OpenGateway(g)
      within(500 millis) {
        expectMsg(WayFound(g, "/var/cache/foo"))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "close a gateway" >> {
      processor.process("threshold" :: "close" :: "10.16.15.43/public" :: "frog/sheep/cow" :: Nil) returns(processing)
      processing.waitFor returns(Result(true, Nil, -1))
      val x = TestActorRef(new Threshold(processor)).start
      x ! CloseGateway(g)
      within(500 millis) {
        expectMsg(WayLost(g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "fail to open a gateway if process errrors" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns(Result(false, "Gate opened '/var/cache/foo'" :: Nil, -1))
      val x = TestActorRef(new Threshold(processor)).start
      x ! OpenGateway(g)
      within(500 millis) {
        expectMsg(WayLost(g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "fail to open a gateway if local path not detected" in {
      processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns(Result(true, "Moo cow /var/cache/foo" :: Nil, -1))
      val x = TestActorRef(new Threshold(processor)).start
      x ! OpenGateway(g)
      within(500 millis) {
        expectMsg(WayLost(g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }
  }
}
