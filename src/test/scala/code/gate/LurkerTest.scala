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
  }
  "Lurker" should {
    def queryG(): Gateway = transaction { Mythos.gateways.lookup(1L) getOrElse null }
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
        x !? (500, Ping)
        g = queryG()
        g.state must beEqual(GateState.open)
        g.localPath must beMatching("/srv/f")
      }
      "on way lost" in {
        var g = queryG()
        x ! WayLost(g)
        x !? (500, Ping)
        g = queryG()
        g.state must beEqual(GateState.lost)
      }
    }
    "parse artifacts on way found" in {
      "adding new artifacts" in {
        transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: Nil)
        var g = queryG()
        x ! WayFound(g, "/srv/g")
        x !? (500, Ping)
        val as = transaction { g.artifacts toList }
        as must haveSize(2)

        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: "readme.nfo" :: Nil)
        x ! WayFound(g, "/srv/g")
        x !? (500, Ping)
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
