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
import code.model.Mythos._
import code.model._
import org.squeryl.PrimitiveTypeMode._

class ClonerTestSpecsAsTest extends JUnit4(ClonerTestSpecs)
object ClonerTestSpecsRunner extends ConsoleRunner(ClonerTestSpecs)

object ClonerTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.init
  }
  "Cloner" should {
    val x = new ClonerComponentImpl with ProcessorComponent with ManipulatorComponent {
      val processor = mock[Processor]
      val manipulator = mock[Manipulator]
    }
    val processing = mock[Processing]
    "start" >> {
      val job: Clone = transaction {
        clones.insert(Clone.create(TestDb.c1ga1.id, TestDb.c2.id, CloneState.queued))
      }
      x.processor.process(any[List[String]]) returns(processing)
      processing.waitFor returns(Result(true, "hlhhklhlkjhkjhlkjhlh" :: Nil, -1))
      x.cloner.start(job)
      there was one(x.processor).process("cloner" :: "/tmp/cache/gate/cow/glue" :: "/tmp/cache/gate/goat/clones" :: Nil)
    }
  }
}
