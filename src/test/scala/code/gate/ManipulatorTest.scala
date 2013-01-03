package code.gate

import org.specs._
import org.specs.mock.Mockito
import TestDb
import model._

import model.Mythos._
import org.squeryl.PrimitiveTypeMode._

object ManipulatorTestSpecs extends Specification with Mockito {
  class ManipulatorComponentTestImpl extends ManipulatorComponentImpl with ClonerComponent with PresenterComponent {
    val presenter = mock[Presenter]
    val cloner = mock[Cloner]
    presenter.currently returns(None)
    cloner.currently returns(None)
  }

  val db = new TestDb

  doBeforeSpec {
    db.init
    db.reset
  }

  "Manipulator" should {
    "wake and do nothing if no waiting clones" >> {
      transaction {
        clones.delete(from(clones)(c => select(c)))
        val x = new ManipulatorComponentTestImpl
        x.manipulator.start
        x.manipulator ! Wake
        there was no(x.cloner).start(any[Clone])
        there was no(x.cloner).cancel
      }
    }

//    "with waiting clones, do nothing if no valid destination" >> {
//      transaction {
//        clones.delete(from(clones)(c => select(c)))
//        clones.insert(Clone.create(db.c1ga2.id, db.c2.id, CloneState.awaiting))
//      }
//      transaction {
//        db.c1g.state = GateState.open
//        db.c2g.mode = GateMode.source
//        db.c2g.state = GateState.open
//        gateways.update(db.c1g :: db.c2g :: Nil)
//      }
//
//      val x = new ManipulatorComponentTestImpl
//      x.manipulator.start
//      x.manipulator ! Wake
//      x.manipulator !? (1000, Ping)
//      there was no(x.cloner).start(any[Clone])
//      there was no(x.cloner).cancel
//
//      transaction {
//        db.c1g.state = GateState.open
//        db.c2g.mode = GateMode.sink
//        db.c2g.state = GateState.lost
//        gateways.update(db.c1g :: db.c2g :: Nil)
//      }
//
//      x.manipulator ! Wake
//      x.manipulator !? (1000, Ping)
//      there was no(x.cloner).start(any[Clone])
//      there was no(x.cloner).cancel
//
//      transaction {
//        db.c1g.state = GateState.lost
//        db.c2g.mode = GateMode.sink
//        db.c2g.state = GateState.open
//        gateways.update(db.c1g :: db.c2g :: Nil)
//      }
//
//      x.manipulator ! Wake
//      x.manipulator !? (1000, Ping)
//      there was no(x.cloner).start(any[Clone])
//      there was no(x.cloner).cancel
//    }
//
//    "with waiting clones, start first with valid source (open) destination (rw, open)" >> {
//      transaction {
//        clones.delete(from(clones)(c => select(c)))
//        val cloneB: Clone = clones.insert(Clone.create(db.c1ga1.id, db.c2.id, CloneState.awaiting))
//        val cloneA: Clone = clones.insert(Clone.create(db.c1ga2.id, db.c2.id, CloneState.awaiting))
//        db.c1g.state = GateState.open
//        db.c2g.mode = GateMode.sink
//        db.c2g.state = GateState.open
//        gateways.update(db.c1g :: db.c2g :: Nil)
//      }
//
//      val x = new ManipulatorComponentTestImpl
//      x.manipulator.start
//      x.manipulator ! Wake
//      x.manipulator !? (1000, Ping)
//      there was one(x.cloner).currently
//      there was one(x.cloner).start(any[Clone])
//      there was no(x.cloner).cancel
//    }
//
//    "yet not start if the cloner is already busy" >> {
//      transaction {
//        clones.delete(from(clones)(c => select(c)))
//        val cloneB: Clone = clones.insert(Clone.create(db.c1ga1.id, db.c2.id, CloneState.awaiting))
//        val cloneA: Clone = clones.insert(Clone.create(db.c1ga2.id, db.c2.id, CloneState.awaiting))
//        db.c2g.mode = GateMode.sink
//        db.c2g.state = GateState.open
//        gateways.update(db.c2g)
//      }
//
//      val x = new ManipulatorComponentTestImpl
//      x.cloner.currently returns(Some(new Clone()))
//      x.manipulator.start
//      x.manipulator ! Wake
//      x.manipulator !? (1000, Ping)
//      there was one(x.cloner).currently
//      there was no(x.cloner).start(any[Clone])
//      there was no(x.cloner).cancel
//    }

    "cancel cloning if withdrawn" >> {
      val x = new ManipulatorComponentTestImpl
      x.manipulator.start
      x.manipulator ! Withdraw
      x.manipulator !? (1000, Ping)
      there was no(x.presenter).start(any[Presence])
      there was no(x.cloner).start(any[Clone])
      there was one(x.presenter).cancel
      there was one(x.cloner).cancel
    }
  }

  doAfterSpec { db.close }
}
