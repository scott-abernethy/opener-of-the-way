package code.model

import org.specs.runner.{ConsoleRunner, JUnit4}
import org.specs.Specification
import org.specs.mock.Mockito
import code.TestDb
import code.model.Mythos._
import code.gate.T
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp

object CloneSnapshotFactoryTest extends Specification with Mockito {
  val db = new TestDb
  db.init

  doBeforeSpec {
    db.reset
    inTransaction{
      val time1 = T.at(2011, 3, 20, 1, 2, 3)
      val time2 = T.at(2011, 3, 22, 1, 2, 3)
      artifacts.delete(from(artifacts)(a => select(a)))
      artifacts.insert(new Artifact(1L, "a/b/c", time1, T.now))
      artifacts.insert(new Artifact(2L, "fudge", time1, T.now))
      artifacts.insert(new Artifact(1L, "d/e/f", time2, T.now))
      artifacts.insert(new Artifact(2L, "sugar", time2, T.now))
      artifacts.insert(new Artifact(2L, "chocolate", time2, T.now))
    }
  }

  "CloneSnapshotFactory" should {

    "be empty for no clones" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new CloneSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must beEmpty
      xx.cloned must beEmpty
    }

    "only show artifacts cloned by cultist" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloning)))
      val x = new CloneSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must haveSize(1)
      xx.cloned must haveSize(0)
    }

    "only show artifacts cloned by cultist, in the last 7 days" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = Clone.create(i, 2L, CloneState.cloned)
      myClone.attempted = T.ago(6 * 24 * 60 * 60 * 1000)
      val myOldClone = Clone.create(i+1, 2L, CloneState.cloned)
      myOldClone.attempted = T.ago((7 * 24 * 60 * 60 * 1000) + 1)
      inTransaction {
        clones.insert(myClone)
        clones.insert(myOldClone)
      }

      val x = new CloneSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must haveSize(0)
      xx.cloned must haveSize(1)
    }

    "ordered by non-completes by request date, then completes by completion date" >> {
      inTransaction {
//        artifacts.delete(from(artifacts)(a => select(a)))
        val a1 = artifacts.insert(new Artifact(1L, "aa", T.now, T.now))
        val a2 = artifacts.insert(new Artifact(1L, "bb", T.now, T.now))
        val a3 = artifacts.insert(new Artifact(1L, "cc", T.now, T.now))
        val a4 = artifacts.insert(new Artifact(1L, "dd", T.now, T.now))
        val a5 = artifacts.insert(new Artifact(1L, "ee", T.now, T.now))

        clones.delete(from(clones)(c => select(c)))
        clones.insert(Clone.fake(a3.id, 2L, CloneState.awaiting, new Timestamp(600000), T.yesterday))
        clones.insert(Clone.fake(a2.id, 2L, CloneState.cloning, new Timestamp(700000), T.yesterday))
        clones.insert(Clone.fake(a5.id, 2L, CloneState.cloned, new Timestamp(700000), T.ago(3 * 24 * 60 * 60 * 1000)))
        clones.insert(Clone.fake(a4.id, 2L, CloneState.awaiting, new Timestamp(610000), T.yesterday))
        clones.insert(Clone.fake(a1.id, 2L, CloneState.cloned, new Timestamp(800000), T.ago(4 * 24 * 60 * 60 * 1000)))

        val x = new CloneSnapshotFactory
        val xx = x.create(2)
        xx.awaiting must haveSize(3)
        xx.awaiting(0)._1 must be_==(a2)
        xx.awaiting(1)._1 must be_==(a4)
        xx.awaiting(2)._1 must be_==(a3)

        xx.cloned must haveSize(2)
        xx.cloned(0)._1 must be_==(a5)
        xx.cloned(1)._1 must be_==(a1)
      }
    }

    /*
    "default state to mine or available if no clones exist" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new CloneSnapshotFactory
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactState.glimpsed)
      x.states.get(i + 1) must beSome(ArtifactState.proffered)
      x.states.get(i + 2) must beSome(ArtifactState.glimpsed)
      x.states.get(i + 3) must beSome(ArtifactState.proffered)
      x.states.get(i + 4) must beSome(ArtifactState.proffered)
      x.states.get(i + 5) must beNone
    }
    "reflect clone state" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloning)))
      val x = new CloneSnapshotFactory
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.awaiting)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.proffered)

      myClone.state = CloneState.cloning
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.cloning)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.proffered)

      myClone.state = CloneState.cloned
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.cloned)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.proffered)
    }
    "allow artifacts to be updated" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new CloneSnapshotFactory
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactState.glimpsed)

      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      x.update(i)
      x.states.get(i) must beSome(ArtifactState.awaiting)
    }
    "not exclude items cloned by others (bug)" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))

      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L

      inTransaction(clones.insert(Clone.create(i + 2, 3L, CloneState.cloning)))
      inTransaction(clones.insert(Clone.create(i + 3, 3L, CloneState.awaiting)))

      val x = new CloneSnapshotFactory
      x.reload(2)
      
      x.states.get(i) must beSome(ArtifactState.glimpsed)
      x.states.get(i + 1) must beSome(ArtifactState.proffered)
      x.states.get(i + 2) must beSome(ArtifactState.glimpsed)
      x.states.get(i + 3) must beSome(ArtifactState.proffered)
      x.states.get(i + 4) must beSome(ArtifactState.proffered)
      x.states.get(i + 5) must beNone
    }
    */
  }

  doAfter {
    db.close
  }
}
