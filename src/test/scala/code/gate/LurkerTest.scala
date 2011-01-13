package code.gate

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

import org.squeryl.PrimitiveTypeMode._

class LurkerTestSpecsAsTest extends JUnit4(LurkerTestSpecs)
object LurkerTestSpecsRunner extends ConsoleRunner(LurkerTestSpecs)

object LurkerTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.init
    transaction {
      val c = new Cultist("bob@bob.com", "")
      Mythos.cultists.insert(c)
      val g = new Gateway(c.id, "10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.rw, GateState.lost)
      Mythos.gateways.insert(g) 
    }
  }
  "Lurker" should {
    def queryG(): Gateway = transaction { Mythos.gateways.where(x => x.path === "frog/sheep/cow") single }
    object LurkerComponentTest extends LurkerComponentImpl with FileSystemComponent {
      val fileSystem = mock[FileSystem]
    }
    val fileSystem = LurkerComponentTest.fileSystem
    fileSystem.find("/srv/f") returns(Nil)
    
    val x = LurkerComponentTest.lurker.start
    "update a gateway state" in {
      "on way found" in {
        var g = queryG()
        x ! WayFound(g, "/srv/f")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        g = queryG()
        g.state must beEqual(GateState.open)
        g.localPath must beMatching("/srv/f")
      }
      "on way lost" in {
        var g = queryG()
        x ! WayLost(g)
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        g = queryG()
        g.state must beEqual(GateState.lost)
      }
    }
    "parse artifacts on way found" in {
      "adding new artifacts" in {
        // ensure no artifacts exist?
        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: Nil)
        var g = queryG()
        x ! WayFound(g, "/srv/g")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        val as = transaction { g.artifacts toList }
        as must haveSize(2)

        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: "readme.nfo" :: Nil)
        x ! WayFound(g, "/srv/g")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        val as2 = transaction { g.artifacts toList }
        as2 must haveSize(3)
      }
      "removing missing artifacts" in {}
      "cancelling bad copy jobs" in {}
    }
    "activate outstanding copies on way found" in {}
  }
  doAfterSpec {
    TestDb.close
  }
}
