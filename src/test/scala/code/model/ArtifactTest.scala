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

import net.liftweb.squerylrecord.RecordTypeMode._
  
class ArtifactTestSpecsAsTest extends JUnit4(ArtifactTestSpecs)
object ArtifactTestSpecsRunner extends ConsoleRunner(ArtifactTestSpecs)
object ArtifactTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.open
    TestDb.use { _ =>
      val c = Cultist.createRecord.email("bob@bob.com")
      cultists.insert(c)
      val c2 = Cultist.createRecord.email("sue@sue.com")
      cultists.insert(c2)
      val g = Gateway.createRecord.cultistId(c.id).location("10.16.15.43/public").path("frog/sheep/cow").password("cowsaregreen").state(GateState.lost)
      gateways.insert(g)
      val now = java.util.Calendar.getInstance
      val x = Artifact.createRecord.gatewayId(g.id).path("a/b/c").discovered(now).witnessed(now)
      artifacts.insert(x)
    }
  }
  "Artifact" should {
    val c: Cultist = Mythos.cultists.lookup(1L) getOrElse null
    val c2: Cultist = Mythos.cultists.lookup(2L) getOrElse null
    val x: Artifact = Mythos.artifacts.lookup(1L) getOrElse null
    "resolve it's owning cultist" in {
      x.owner must beSome(c)
      x.owner must beSome(c)
    }
    "be mine if owning cultist is logged in" in {
      x.stateFor(c) must beSome(ArtifactState.mine)
    }
    "be available if not mine" in {
      x.stateFor(c2) must beSome(ArtifactState.available)
    }
    "be waiting if waiting clone exists" in {
      clones.delete(clones.where(cl => cl.artifactId.is === x.id))
      clones.insert(Clone.createRecord.artifactId(x.id).forCultistId(c2.id).state(CloneState.waiting))
      x.stateFor(c2) must beSome(ArtifactState.waiting)
    }
    "be progressing if progressing clone exists" in {
      clones.delete(clones.where(cl => cl.artifactId.is === x.id))
      clones.insert(Clone.createRecord.artifactId(x.id).forCultistId(c2.id).state(CloneState.progressing))
      x.stateFor(c2) must beSome(ArtifactState.progressing)
    }
  }
  doAfterSpec {
    TestDb.close
  }
}
