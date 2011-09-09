package code.gate

import org.specs.Specification
import code.TestDb
import org.specs.mock.Mockito
import akka.testkit.TestActorRef._
import org.squeryl.PrimitiveTypeMode._
import akka.testkit.{TestActorRef, TestKit}
import akka.util.duration._
import code.model._

object WatcherTest extends Specification with Mockito with TestKit {

  val db = new TestDb

  doBeforeSpec {
    db.init
    db.reset
  }

  "Watcher" should {

    "handle wake" >> {
      val x = TestActorRef(new Watcher(testActor))
      x.isDefinedAt('Wake) must be (true)
    }

    "open gateways that need to be scoured" >> {
      transaction {
        db.c1g.mode = GateMode.source
        db.c1g.state = GateState.inactive
        db.c1g.scoured = T.ago((3 * 60 * 60 * 1000) + 1)
        db.c2g.mode = GateMode.source
        db.c2g.state = GateState.inactive
        db.c2g.scoured = T.now
        db.c3g.mode = GateMode.sink
        db.c3g.state = GateState.inactive
        db.c3g.scoured = T.yesterday
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        Mythos.gateways.update(db.c3g)
      }
      val x = TestActorRef(new Watcher(testActor)).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c1g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "open gateways required for clones" >> {
      transaction {
        db.c1g.mode = GateMode.source
        db.c1g.state = GateState.inactive
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.inactive
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        val c = new Clone()
        c.artifactId = db.c1ga1.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
      }
      val x = TestActorRef(new Watcher(testActor)).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c1g))
        expectMsg(OpenGateway(db.c2g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "close gateways no longer required" >> {
      transaction {
        db.c1g.state = GateState.open
        db.c1g.scoured = T.yesterday
        db.c2g.state = GateState.open
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        Mythos.clones.delete(from(Mythos.clones)(c => select(c)))
      }
      val x = TestActorRef(new Watcher(testActor)).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(CloseGateway(db.c2g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }
  }
}