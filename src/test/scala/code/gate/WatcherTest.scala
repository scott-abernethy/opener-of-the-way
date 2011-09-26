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
        db.c1g.state = GateState.closed
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.closed
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
        db.c1g.state = GateState.closed
        db.c1g.scoured = T.ago((3 * 60 * 60 * 1000) + 1000)
        db.c2g.mode = GateMode.source
        db.c2g.state = GateState.closed
        db.c2g.scoured = T.now
        db.c3g.mode = GateMode.sink
        db.c3g.state = GateState.closed
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
        db.c1g.state = GateState.closed
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.closed
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
        db.c1g.state = GateState.closed
        db.c1g.scoured = T.now
        db.c2g.mode = GateMode.sink
        db.c2g.state = GateState.closed
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

    "mark gateways no longer required as transient" >> {
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
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
      transaction ( Mythos.gateways.where(g => g.id === db.c2g.id).head.state ) must be_==(GateState.transient)
    }

//    "check open gateways are still open" >> {
//
//    }

    "close transient gateways" >> {
      db.reset
      transaction {
        db.c1g.state = GateState.transient
        db.c1g.scoured = T.now
        db.c2g.state = GateState.closed
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
      }
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor {})).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(CloseGateway(db.c1g))
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "close transient gateways, but not if any clones are cloning (to be safe)" >> {
      db.reset
      transaction {
        db.c1g.state = GateState.transient
        db.c1g.scoured = T.now
        db.c2g.state = GateState.open
        db.c2g.scoured = T.now
        Mythos.gateways.update(db.c1g)
        Mythos.gateways.update(db.c2g)
        val c = new Clone()
        c.artifactId = db.c1ga1.id
        c.forCultistId = db.c2.id
        c.state = CloneState.cloning
        c.attempts = 0
        Mythos.clones.insert(c)
      }
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor {})).start
      within(500 millis) {
        x ! 'Wake
        expectMsg(OpenGateway(db.c2g)) // because there is a clone
        expectNoMsg
      }
      x.getMailboxSize() must be_==(0)
    }

    "update a gateway state, on way found" >> {
      db.reset
      var g = transaction( db.c1g )
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor {})).start
      within(500 millis) {
        x ! OpenGateSuccess(g, "/srv/f")
        x ! 'Ping
        expectMsg('Pong)
      }
      g = transaction( Mythos.gateways.lookup(g.id).getOrElse(null) )
      g.state must beEqual(GateState.open)
      g.localPath must beMatching("/srv/f")
    }

    "update a gateway state, on way lost, inactive" >> {
      db.reset
      var g = transaction( db.c1g )
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor {})).start
      db.c1g.seen = T.ago(3*24*60*60*1000) // 3 days ago
      // TODO only pass if that is in db.
      within(500 millis) {
        x ! CloseGateSuccess(g)
        x ! 'Ping
        expectMsg('Pong)
      }
      g = transaction( Mythos.gateways.lookup(g.id).getOrElse(null) )
      g.state must beEqual(GateState.closed)
    }

    "update a gateway state, on way lost, truely lost" >> {
      db.reset
      var g = transaction( db.c1g )
      val x = TestActorRef(new Watcher(testActor, scala.actors.Actor.actor {})).start
      db.c1g.seen = T.ago((4*24*60*60*1000) + 1) // 4 days ago
      // TODO only pass if that is in db.
      within(500 millis) {
        x ! CloseGateSuccess(g)
        x ! 'Ping
        expectMsg('Pong)
      }
      g = transaction( Mythos.gateways.lookup(g.id).getOrElse(null) )
      g.state must beEqual(GateState.lost)
    }

  }
}