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
import code.TestDb._
import code.model._

import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._

class ManipulatorTestSpecsAsTest extends JUnit4(ManipulatorTestSpecs)
object ManipulatorTestSpecsRunner extends ConsoleRunner(ManipulatorTestSpecs)

object ManipulatorTestSpecs extends Specification with Mockito {
  class ManipulatorComponentTestImpl extends ManipulatorComponentImpl with ClonerComponent {
    val cloner = mock[Cloner]
    cloner.currently returns(None)
  }
  doBeforeSpec { TestDb.init }
  "Manipulator" should {
    "wake and do nothing if no waiting clones" in {
      transaction {
        clones.delete(from(clones)(c => select(c)))
        val x = new ManipulatorComponentTestImpl
        x.manipulator.start
        x.manipulator ! Wake
        there was no(x.cloner).start(any[Clone])
        there was no(x.cloner).cancel
      }
    }
    "with waiting clones, do nothing if no valid destination" in {
      transaction {
        clones.delete(from(clones)(c => select(c)))
        clones.insert(new Clone(c1ga2.id, c2.id, CloneState.waiting))
      }
      transaction {
        c1g.state = GateState.open
        c2g.mode = GateMode.ro
        c2g.state = GateState.open
        gateways.update(c1g :: c2g :: Nil)
      }

      val x = new ManipulatorComponentTestImpl
      x.manipulator.start
      x.manipulator ! Wake
      x.manipulator !? (1000, Ping)
      there was no(x.cloner).start(any[Clone])
      there was no(x.cloner).cancel

      transaction {
        c1g.state = GateState.open
        c2g.mode = GateMode.rw
        c2g.state = GateState.lost
        gateways.update(c1g :: c2g :: Nil)
      }

      x.manipulator ! Wake
      x.manipulator !? (1000, Ping)
      there was no(x.cloner).start(any[Clone])
      there was no(x.cloner).cancel

      transaction {
        c1g.state = GateState.lost
        c2g.mode = GateMode.rw
        c2g.state = GateState.open
        gateways.update(c1g :: c2g :: Nil)
      }

      x.manipulator ! Wake
      x.manipulator !? (1000, Ping)
      there was no(x.cloner).start(any[Clone])
      there was no(x.cloner).cancel
    }
    "with waiting clones, start first with valid source (open) destination (rw, open)" in {
      transaction {
        clones.delete(from(clones)(c => select(c)))
        val cloneB: Clone = clones.insert(new Clone(c1ga1.id, c2.id, CloneState.waiting))
        val cloneA: Clone = clones.insert(new Clone(c1ga2.id, c2.id, CloneState.waiting))
        c1g.state = GateState.open
        c2g.mode = GateMode.rw
        c2g.state = GateState.open
        gateways.update(c1g :: c2g :: Nil)
      }

      val x = new ManipulatorComponentTestImpl
      x.manipulator.start
      x.manipulator ! Wake
      x.manipulator !? (1000, Ping)
      there was one(x.cloner).currently
      there was one(x.cloner).start(any[Clone])
      there was no(x.cloner).cancel
    }
    "yet not start if the cloner is already busy" in {
      transaction {
        clones.delete(from(clones)(c => select(c)))
        val cloneB: Clone = clones.insert(new Clone(c1ga1.id, c2.id, CloneState.waiting))
        val cloneA: Clone = clones.insert(new Clone(c1ga2.id, c2.id, CloneState.waiting))
        c2g.mode = GateMode.rw
        c2g.state = GateState.open
        gateways.update(c2g)
      }

      val x = new ManipulatorComponentTestImpl
      x.cloner.currently returns(Some(new Clone()))
      x.manipulator.start
      x.manipulator ! Wake
      x.manipulator !? (1000, Ping)
      there was one(x.cloner).currently
      there was no(x.cloner).start(any[Clone])
      there was no(x.cloner).cancel
    }
    "cancel cloning if withdrawn" in {
      val x = new ManipulatorComponentTestImpl
      x.manipulator.start
      x.manipulator ! Withdraw
      x.manipulator !? (1000, Ping)
      there was no(x.cloner).start(any[Clone])
      there was one(x.cloner).cancel
    }
  }
  doAfterSpec { TestDb.close }
}
