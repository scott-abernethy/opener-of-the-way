package code.gate

import org.specs.Specification
import org.specs.mock.Mockito
import akka.testkit.TestKit
import TestDb
import org.squeryl.PrimitiveTypeMode._
import model.{Mythos, CloneState, Clone}
import akka.testkit.TestActorRef
import akka.testkit.TestActorRef._
import akka.util.duration._
import model._

object SummonerTest extends Specification with Mockito with TestKit {

  val db = new TestDb
  db.init

  "Summoner" should {

    "call presence for new clones" >> {
      db.reset
      transaction {
        val c = new Clone()
        c.artifactId = db.c1ga2.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
      }

      val x = TestActorRef(new Summoner(scala.actors.Actor.actor{}, testActor))
      x.isDefinedAt('Wake) must be (true)
      x.start()
      
      within(900 millis) {
        x ! 'Wake
        expectMsg('Source)
      }

      val p = transaction ( Mythos.artifactToPresences.left(db.c1ga2).headOption )
      p.map(_.artifactId) must beSome(db.c1ga2.id)
      p.map(_.state) must beSome(PresenceState.called)
      p.map(_.attempts) must beSome(0)
    }

    "call presence for clones, where an unknown presence exists" >> {
      db.reset
      transaction {
        val c = new Clone()
        c.artifactId = db.c1ga2.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
        val p = new Presence()
        p.artifactId = db.c1ga2.id
        p.state = PresenceState.unknown
        p.attempts = 4
        Mythos.presences.insert(p)
      }

      val x = TestActorRef(new Summoner(scala.actors.Actor.actor{}, testActor))
      x.isDefinedAt('Wake) must be (true)
      x.start()

      within(900 millis) {
        x ! 'Wake
        expectMsg('Source)
      }

      val p = transaction ( Mythos.artifactToPresences.left(db.c1ga2).headOption )
      p.map(_.artifactId) must beSome(db.c1ga2.id)
      p.map(_.state) must beSome(PresenceState.called)
      p.map(_.attempts) must beSome(4)
    }

    "where presence exists, wake has no affect, but summon leads to sink notification" >> {
      db.reset
      transaction {
        val c = new Clone()
        c.artifactId = db.c1ga2.id
        c.forCultistId = db.c2.id
        c.state = CloneState.awaiting
        c.attempts = 0
        Mythos.clones.insert(c)
        val p = new Presence()
        p.artifactId = db.c1ga2.id
        p.state = PresenceState.present
        p.attempts = 4
        Mythos.presences.insert(p)
      }

      val x = TestActorRef(new Summoner(scala.actors.Actor.actor{}, testActor))
      x.isDefinedAt('Wake) must be (true)
      x.start()

      within(900 millis) {
        x ! 'Wake
        expectNoMsg
      }

      within(900 millis) {
        x ! Summon(db.c1ga2.id)
        expectMsg('Sink)
      }

      val p = transaction ( Mythos.artifactToPresences.left(db.c1ga2).headOption )
      p.map(_.artifactId) must beSome(db.c1ga2.id)
      p.map(_.state) must beSome(PresenceState.present)
      p.map(_.attempts) must beSome(4)
    }

    "where too much presence, release oldest artifact" >> {
      db.reset
      transaction {
        Mythos.presences.insert( List( presentAt(db.c3a1), presentAt(db.c3a2), presentAt(db.c3a3), presentAt(db.c3a4) ) )
      }

      val x = TestActorRef(new Summoner(scala.actors.Actor.actor{}, testActor)).start()
      within(900 millis) {
        x ! 'Wake
        x ! 'Ping
        expectMsg('Pong)
      }

      transaction {
        Presence.forArtifact(db.c3a1.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a3.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a4.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a2.id).headOption.map(_.state) must beSome(PresenceState.released)
      }
    }

    "where too much presence, release oldest artifact not awaiting" >> {
      db.reset
      transaction {
        Mythos.presences.insert( List( presentAt(db.c3a1), presentAt(db.c3a2), presentAt(db.c3a3), presentAt(db.c3a4) ) )
        Mythos.clones.insert( Clone.create(db.c3a2.id, db.c2.id, CloneState.awaiting) )
      }

      val x = TestActorRef(new Summoner(scala.actors.Actor.actor{}, testActor)).start()
      within(900 millis) {
        x ! 'Wake
        x ! 'Ping
        expectMsg('Pong)
      }

      transaction {
        Presence.forArtifact(db.c3a1.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a2.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a4.id).headOption.map(_.state) must beSome(PresenceState.present)
        Presence.forArtifact(db.c3a3.id).headOption.map(_.state) must beSome(PresenceState.released)
      }
    }

    "remove filesystem presence when released" >> {}

    "check stuff" >> {}
  }

  def presentAt(a: Artifact) = {
    val p = new Presence()
    p.artifactId = a.id
    p.state = PresenceState.present
    p.attempts = 1
    p
  }



}