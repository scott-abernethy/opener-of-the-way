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

import net.liftweb.squerylrecord.RecordTypeMode._

class LurkerTestSpecsAsTest extends JUnit4(LurkerTestSpecs)
object LurkerTestSpecsRunner extends ConsoleRunner(LurkerTestSpecs)

object LurkerTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.open
    TestDb.use { _ =>
      val c = Cultist.createRecord.name("Bob")
      Mythos.cultists.insert(c)
      val g = Gateway.createRecord.cultistId(c.id).location("10.16.15.43/public").path("frog/sheep/cow").password("cowsaregreen").state(GateState.lost)
      Mythos.gateways.insert(g) 
    }
  }
  "Lurker" should {
    def queryG(): Gateway = TestDb.use { _ => Mythos.gateways.where(x => x.path === "frog/sheep/cow") single }
    val fileSystem = mock[FileSystem]
    fileSystem.find("/srv/f") returns(Nil)
    val x = new Lurker(fileSystem).start
    "update a gateway state" in {
      "on way found" in {
        var g = queryG()
        x ! WayFound(g, "/srv/f")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        g = queryG()
        g.state.is must beEqual(GateState.open)
        g.localPath.is must beMatching("/srv/f")
      }
      "on way lost" in {
        var g = queryG()
        x ! WayLost(g)
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        g = queryG()
        g.state.is must beEqual(GateState.lost)
      }
    }
    "parse artifacts on way found" in {
      "adding new artifacts" in {
        // ensure no artifacts exist?
        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: Nil)
        var g = queryG()
        x ! WayFound(g, "/srv/g")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        val as = TestDb.use { _ => g.artifacts toList }
        as must haveSize(2)

        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: "readme.nfo" :: Nil)
        x ! WayFound(g, "/srv/g")
        Thread.sleep(500) // super lame. need to wait for Lurker to finish
        val as2 = TestDb.use { _ => g.artifacts toList } 
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
  /*
  "Threshold" should {
    val processor = mock[Processor]
    val g = Gateway.createRecord.location("10.16.15.43/public").path("frog/sheep/cow").password("cowsaregreen")
    val x = new Threshold(g, self, processor).start
    "open a gateway" in {
      processor.waitFor("threshold" :: "open" :: "10.16.15.43/public" :: "frog/sheep/cow" :: "cowsaregreen" :: Nil) returns((true, ">>> '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayFound(g2, lp) => 
          g2.location.is must be("10.16.15.43/public")
          lp must beMatching("/var/cache/foo")
        case _ => fail
      }
    }
    "close a gateway" in {
      processor.waitFor("threshold" :: "close" :: "10.16.15.43/public" :: "frog/sheep/cow" :: Nil) returns((true, Nil))
      x ! Close()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if process errrors" in {
      processor.waitFor(any[List[String]]) returns((false, ">>> '/var/cache/foo'" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }
    "fail to open a gateway if local path not detected" in {
      processor.waitFor(any[List[String]]) returns((true, "$$$ /var/cache/foo" :: Nil))
      x ! Open()
      self.receiveWithin(1000) {
        case WayLost(g2) => 
          g2.location.is must be("10.16.15.43/public")
        case _ => fail
      }
    }
    */
}
