package code.model

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

import code.TestDb
import code.model._
import code.model.Mythos._

import org.squeryl.PrimitiveTypeMode._
import code.gate.T

class ArtifactTestSpecsAsTest extends JUnit4(ArtifactTestSpecs)
object ArtifactTestSpecsRunner extends ConsoleRunner(ArtifactTestSpecs)
object ArtifactTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.init
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
        x.stateFor(c) must beSome(ArtifactState.mine)
      }
    }
    "be available if not mine" >> {
      transaction {
        x.stateFor(c2) must beSome(ArtifactState.available)
      }
    }
    "be waiting if waiting clone exists" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.waiting, 0))
        x.stateFor(c2) must beSome(ArtifactState.waiting)
      }
    }
    "be progressing if progressing clone exists" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.progressing, 0))
        x.stateFor(c2) must beSome(ArtifactState.progressing)
      }
    }
    "start waiting clone requests from available state" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        x.stateFor(c2) must beSome(ArtifactState.available)
        x.clone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.waiting)
      }
    }
    "cancel existing clone requests" >> {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.waiting, 0))
        x.stateFor(c2) must beSome(ArtifactState.waiting)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.available)

        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.progressing, 0))
        x.stateFor(c2) must beSome(ArtifactState.progressing)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.available)
      }
    }
    "have a local path based on the gateway local path" >> {
      transaction {
        val g: Gateway = gateways.insert(new Gateway(TestDb.c1.id, "foo", "bar", "/tmp/g/it", "password", GateMode.rw, GateState.open))
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
  }
  doAfterSpec {
    TestDb.close
  }
}
