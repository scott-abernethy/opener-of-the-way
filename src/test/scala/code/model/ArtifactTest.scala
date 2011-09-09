package code.model

import org.specs._
import org.specs.mock.Mockito
import code.TestDb
import code.model.Mythos._

import org.squeryl.PrimitiveTypeMode._
import code.gate.T

object ArtifactTest extends Specification with Mockito {

  val db = new TestDb
  db.init

  doBeforeSpec {
    db.reset
  }

  "Artifact" should {

    val (c: Cultist, c2: Cultist, x: Artifact) = transaction {
      (cultists.lookup(1L) getOrElse null,
      cultists.lookup(2L) getOrElse null,
      artifacts.lookup(1L) getOrElse null)
    }

    "resolve it's owning cultist" >> {
      transaction {
        x.owner must beSome(c)
        x.owner must beSome(c)
      }
    }

    "be mine if owning cultist is logged in" >> {
      transaction {
        x.stateFor(c) must beSome(ArtifactState.proffered)
      }
    }

    "be available if not mine" >> {
      transaction {
        x.stateFor(c2) must beSome(ArtifactState.glimpsed)
      }
    }

    "be waiting if waiting clone exists" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, c2.id, CloneState.awaiting))
        x.stateFor(c2) must beSome(ArtifactState.awaiting)
      }
    }

    "be progressing if progressing clone exists" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, c2.id, CloneState.cloning))
        x.stateFor(c2) must beSome(ArtifactState.cloning)
      }
    }

    "be done if done clone exists" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, c2.id, CloneState.cloned))
        x.stateFor(c2) must beSome(ArtifactState.cloned)
      }
    }

    "start waiting clone requests from available state" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        x.stateFor(c2) must beSome(ArtifactState.glimpsed)
        x.clone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.awaiting)
      }
    }

    "cancel existing clone requests" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, c2.id, CloneState.awaiting))
        x.stateFor(c2) must beSome(ArtifactState.awaiting)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.glimpsed)

        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(Clone.create(x.id, c2.id, CloneState.cloning))
        x.stateFor(c2) must beSome(ArtifactState.cloning)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.glimpsed)
      }
    }

    "have a local path based on the gateway local path" >> {
      transaction {
        val g: Gateway = gateways.insert(new Gateway(db.c1.id, "foo", "bar", "/tmp/g/it", "password", GateMode.sink, GateState.open, "", T.yesterday))
        val a: Artifact = artifacts.insert(new Artifact(g.id, "folder/file.ext", T.now, T.now))
        a.localPath must beSome("/tmp/g/it/folder/file.ext")

        g.localPath = "/var/cache/gates/a/"
        gateways.update(g)
        a.localPath must beSome("/var/cache/gates/a/folder/file.ext")

        a.path = "/x/y/z/readme"
        artifacts.update(a)
        a.localPath must beSome("/var/cache/gates/a/x/y/z/readme")        
      }
    }

    "have lost state if not witnessed recently, with no database transactions" >> {
      val now = T.now
      val threeDaysFromNow = T.agoFrom(now, -3 * 24 * 60 * 60 * 1000L)
      val fourDaysFromNow = T.agoFrom(now, -4 * 24 * 60 * 60 * 1000L)
      val fourDaysOneSecondFromNow = T.agoFrom(now, (-4 * 24 * 60 * 60 * 1000L) - 1L)
      val fiveDaysFromNow = T.agoFrom(now, -5 * 24 * 60 * 60 * 1000L)
      val x = new Artifact
      x.witnessed = now
      val clone = new Clone

      x.stateFor(45L, 45L, None, now) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 4L, None, threeDaysFromNow) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 4L, None, fourDaysFromNow) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 4L, None, fourDaysOneSecondFromNow) must beSome(ArtifactState.profferedLost)
      x.stateFor(4L, 4L, None, fiveDaysFromNow) must beSome(ArtifactState.profferedLost)

      x.stateFor(1L, 2L, None, now) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, threeDaysFromNow) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, fourDaysFromNow) must beSome(ArtifactState.glimpsed)
      x.stateFor(1L, 2L, None, fourDaysOneSecondFromNow) must beSome(ArtifactState.lost)
      x.stateFor(1L, 2L, None, fiveDaysFromNow) must beSome(ArtifactState.lost)

      clone.state = CloneState.awaiting
      x.stateFor(45L, 45L, Some(clone), now) must beSome(ArtifactState.proffered)
      x.stateFor(4L, 45L, Some(clone), now) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(clone), threeDaysFromNow) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(clone), fourDaysFromNow) must beSome(ArtifactState.awaiting)
      x.stateFor(4L, 45L, Some(clone), fourDaysOneSecondFromNow) must beSome(ArtifactState.awaitingLost)
      x.stateFor(4L, 45L, Some(clone), fiveDaysFromNow) must beSome(ArtifactState.awaitingLost)

      clone.state = CloneState.cloning
      x.stateFor(45L, 45L, Some(clone), now) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 13L, Some(clone), now) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(clone), threeDaysFromNow) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(clone), fourDaysFromNow) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(clone), fourDaysOneSecondFromNow) must beSome(ArtifactState.cloning)
      x.stateFor(45L, 13L, Some(clone), fiveDaysFromNow) must beSome(ArtifactState.cloning)

      clone.state = CloneState.cloned
      x.stateFor(45L, 45L, Some(clone), now) must beSome(ArtifactState.proffered)
      x.stateFor(45L, 13L, Some(clone), now) must beSome(ArtifactState.cloned)
    }
  }

  doAfter {
    db.close
  }
}
