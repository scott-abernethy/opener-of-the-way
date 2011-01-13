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
  
class ArtifactTestSpecsAsTest extends JUnit4(ArtifactTestSpecs)
object ArtifactTestSpecsRunner extends ConsoleRunner(ArtifactTestSpecs)
object ArtifactTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.init
    transaction {
      val c = new Cultist("bob@bob.com", "")
      cultists.insert(c)
      val c2 = new Cultist("sue@sue.com", "")
      cultists.insert(c2)
      val g = new Gateway(c.id, "10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.rw, GateState.lost)
      gateways.insert(g)
      val now = new java.sql.Timestamp(new java.util.Date().getTime)
      val x = new Artifact(g.id, "a/b/c", now, now)
      artifacts.insert(x)
    }
  }
  "Artifact" should {
    val (c: Cultist, c2: Cultist, x: Artifact) = transaction {
      (cultists.lookup(1L) getOrElse null,
      cultists.lookup(2L) getOrElse null,
      artifacts.lookup(1L) getOrElse null)
    }
    "resolve it's owning cultist" in {
      transaction {
        x.owner must beSome(c)
        x.owner must beSome(c)
      }
    }
    "be mine if owning cultist is logged in" in {
      transaction {
        x.stateFor(c) must beSome(ArtifactState.mine)
      }
    }
    "be available if not mine" in {
      transaction {
        x.stateFor(c2) must beSome(ArtifactState.available)
      }
    }
    "be waiting if waiting clone exists" in {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.waiting))
        x.stateFor(c2) must beSome(ArtifactState.waiting)
      }
    }
    "be progressing if progressing clone exists" in {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.progressing))
        x.stateFor(c2) must beSome(ArtifactState.progressing)
      }
    }
    "start waiting clone requests from available state" in {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        x.stateFor(c2) must beSome(ArtifactState.available)
        x.clone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.waiting)
      }
    }
    "cancel existing clone requests" in {
      transaction {
        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.waiting))
        x.stateFor(c2) must beSome(ArtifactState.waiting)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.available)

        clones.delete(clones.where(cl => cl.artifactId === x.id))
        clones.insert(new Clone(x.id, c2.id, CloneState.progressing))
        x.stateFor(c2) must beSome(ArtifactState.progressing)

        x.cancelClone(c2) must beEqual(true)
        x.stateFor(c2) must beSome(ArtifactState.available)
      }
    }
  }
  doAfterSpec {
    TestDb.close
  }
}
