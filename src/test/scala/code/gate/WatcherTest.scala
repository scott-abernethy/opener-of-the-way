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
  db.init

  "Watcher Queries" should {

    "clone is not ready if artifact very recently discovered" >> {

      db.reset
      transaction {
        db.c1g.mode = GateMode.source
        db.c1g.state = GateState.inactive
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.inactive
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        db.c1ga1.discovered = T.now
        db.c1ga1.witnessed = T.now
        Mythos.artifacts.update(db.c1ga1)
        val c = new Clone()
        c.artifactId = db.c1ga1.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
      }

      val x = transaction(Watcher.readyClonesQuery().toList)
      x must haveSize(0)
    }

  }

  "Watcher" should {

    "handle wake" >> {
      db.reset
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor{}))
      x.isDefinedAt('Wake) must be (true)
    }

    "open gateways that need to be scoured" >> {
      db.reset
      transaction {
        db.c1g.mode = GateMode.source
        db.c1g.state = GateState.inactive
        db.c1g.scoured = T.ago((3 * 60 * 60 * 1000) + 1000)
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
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor{})).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c1g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "open gateways required for clones" >> {
      db.reset
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
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor{})).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c1g))
        expectMsg(OpenGateway(db.c2g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "open gateways required for clones, but not if the artifact is lost" >> {
      db.reset
      transaction {
        db.c1g.mode = GateMode.source
        db.c1g.state = GateState.inactive
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.inactive
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        db.c1ga1.witnessed = T.ago(Artifact.lostAfter + 1)
        Mythos.artifacts.update(db.c1ga1)
        val c = new Clone()
        c.artifactId = db.c1ga1.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
      }
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor{})).start
      within(500 millis) {
        x ! 'Wake
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "close gateways no longer required" >> {
      db.reset
      transaction {
        db.c1g.state = GateState.open
        db.c1g.scoured = T.yesterday
        db.c2g.state = GateState.open
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
      }
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor{})).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c1g)) // This one IS still required, and so will be reopened.
        expectMsg(CloseGateway(db.c2g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }
  }
}