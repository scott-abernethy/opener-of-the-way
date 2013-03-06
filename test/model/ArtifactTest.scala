package model

import org.specs2.mutable._
import org.specs2.mock.Mockito
import model.Mythos._

import test.WithTestApplication
import gate.T
import db._

class ArtifactTest extends Specification with Mockito {

  "Artifact" should {

    "resolve it's owning cultist" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        TestDb.c1ga1.owner must beSome(TestDb.c1)
        TestDb.c1ga1.owner must beSome(TestDb.c1)
      }
    }

    "be mine if owning cultist is logged in" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        TestDb.c1ga1.stateFor(1L) must beSome(ArtifactState.proffered)
      }
    }

    "be available if not mine" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val x = TestDb.c1ga1
        x.stateFor(2L) must beSome(ArtifactState.glimpsed)
      }
    }

    "be waiting if waiting clone exists" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, 2L, CloneState.awaiting))
        x.stateFor(2L) must beSome(ArtifactState.awaiting)
      }
    }

    "be progressing if progressing clone exists" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, 2L, CloneState.cloning))
        x.stateFor(2L) must beSome(ArtifactState.cloning)
      }
    }

    "be done if done clone exists" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, 2L, CloneState.cloned))
        x.stateFor(2L) must beSome(ArtifactState.cloned)
      }
    }

    "start waiting clone requests from available state" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        x.stateFor(2L) must beSome(ArtifactState.glimpsed)
        x.clone(2L) must beEqualTo(true)
        x.stateFor(2L) must beSome(ArtifactState.awaiting)
      }
    }

    "cancel existing clone requests" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, 2L, CloneState.awaiting))
        x.stateFor(2L) must beSome(ArtifactState.awaiting)

        x.cancelClone(2L) must beEqualTo(true)
        x.stateFor(2L) must beSome(ArtifactState.glimpsed)

        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, 2L, CloneState.cloning))
        x.stateFor(2L) must beSome(ArtifactState.cloning)

        x.cancelClone(2L) must beEqualTo(true)
        x.stateFor(2L) must beSome(ArtifactState.glimpsed)
      }
    }

    "have a local path based on the gateway local path" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      transaction {
        val c = TestDb.c1
        val c2 = TestDb.c2
        val x = TestDb.c1ga1
        var g = new Gateway
        g.cultistId = c.id
        g.location = "foo"
        g.path = "bar"
        g.localPath = "/tmp/g/it"
        g.password = "password"
        g.source = false
        g.sink = true
        g.state = GateState.open
        g = gateways.insert(g)
        val a: Artifact = artifacts.insert(Artifact.create(g.id, "folder/file.ext", T.now, T.now))
        a.localPath must beSome("/tmp/g/it/folder/file.ext")

        g.localPath = "/var/cache/gates/a/"
        gateways.update(g)
        a.localPath must beSome("/var/cache/gates/a/folder/file.ext")

        a.path = "/x/y/z/readme"
        artifacts.update(a)
        a.localPath must beSome("/var/cache/gates/a/x/y/z/readme")
      }
    }

    "have lost state if not witnessed recently, with no database transactions" in new WithTestApplication {
      val now = T.now
      val threeDaysFromNow = T.agoFrom(now, -3 * 24 * 60 * 60 * 1000L)
      val fourDaysFromNow = T.agoFrom(now, -4 * 24 * 60 * 60 * 1000L)
      val fourDaysOneSecondFromNow = T.agoFrom(now, (-4 * 24 * 60 * 60 * 1000L) - 1L)
      val fiveDaysFromNow = T.agoFrom(now, -5 * 24 * 60 * 60 * 1000L)
      val x = new Artifact
      x.witnessed = now
      val c = new Clone
      val present = Some(Presence.create(x.id, PresenceState.present))

      x.stateFor(45L, 45L, None, now, None) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 45L, None, now, present) must beSome(ArtifactState.profferedPresent)
      x.stateFor(4L, 4L, None, threeDaysFromNow, None) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 4L, None, fourDaysFromNow, None) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 4L, None, fourDaysOneSecondFromNow, None) must beSome(ArtifactState.profferedLost)
      x.stateFor(4L, 4L, None, fiveDaysFromNow, None) must beSome(ArtifactState.profferedLost)

      x.stateFor(1L, 2L, None, now, None) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, threeDaysFromNow, None) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, fourDaysFromNow, None) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, fourDaysOneSecondFromNow, None) must beSome(ArtifactState.lost)
      x.stateFor(1L, 2L, None, fiveDaysFromNow, None) must beSome(ArtifactState.lost)

      c.state = CloneState.awaiting
      x.stateFor(45L, 45L, Some(c), now, None) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 45L, Some(c), now, present) must beSome(ArtifactState.profferedPresent)
      x.stateFor(4L, 45L, Some(c), now, None) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(c), now, present) must beSome(ArtifactState.awaitingPresent)
      x.stateFor(4L, 45L, Some(c), threeDaysFromNow, None) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(c), fourDaysFromNow, None) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(c), fourDaysOneSecondFromNow, None) must beSome(ArtifactState.awaitingLost)
      x.stateFor(4L, 45L, Some(c), fiveDaysFromNow, None) must beSome(ArtifactState.awaitingLost)
      x.stateFor(4L, 45L, Some(c), fiveDaysFromNow, present) must beSome(ArtifactState.awaitingPresent)

      c.state = CloneState.cloning
      x.stateFor(45L, 45L, Some(c), now, None) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 13L, Some(c), now, None) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(c), threeDaysFromNow, None) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(c), fourDaysFromNow, None) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(c), fourDaysOneSecondFromNow, None) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(c), fiveDaysFromNow, None) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(c), fiveDaysFromNow, present) must beSome(ArtifactState.cloning)

      c.state = CloneState.cloned
      x.stateFor(45L, 45L, Some(c), now, None) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 13L, Some(c), now, None) must beSome(ArtifactState.cloned)
      x.stateFor(45L, 13L, Some(c), now, present) must beSome(ArtifactState.cloned)
    }
  }
}
