package model

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import model.Mythos._
import gate.T
import java.sql.Timestamp
import db.TestDb
import test.WithTestApplication

class ClonedSnapshotFactoryTest extends Specification with Mockito {

//  step {
//    inTransaction{
//      val time1 = T.at(2011, 3, 20, 1, 2, 3)
//      val time2 = T.at(2011, 3, 22, 1, 2, 3)
//      artifacts.delete(from(artifacts)(a => select(a)))
//      artifacts.insert(Artifact.create(1L, "a/b/c", time1, T.now))
//      artifacts.insert(Artifact.create(2L, "fudge", time1, T.now))
//      artifacts.insert(Artifact.create(1L, "d/e/f", time2, T.now))
//      artifacts.insert(Artifact.create(2L, "sugar", time2, T.now))
//      artifacts.insert(Artifact.create(2L, "chocolate", time2, T.now))
//    }
//  }

  "ClonedSnapshotFactory" should {

    "be empty for no clones" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new ClonedSnapshotFactory
      val xx = x.create(2)
      xx.cloned must beEmpty
    }

    "only show artifacts cloned by cultist" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloning)))
      val x = new ClonedSnapshotFactory
      val xx = x.create(2)
      xx.cloned must haveSize(0)
    }

    "only show artifacts cloned by cultist, in the last 7 days" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
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

      val x = new ClonedSnapshotFactory
      val xx = x.create(2)
      xx.cloned must haveSize(1)
    }

    "ordered by non-completes by request date, then completes by completion date" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      inTransaction {
//        artifacts.delete(from(artifacts)(a => select(a)))
        val a1 = artifacts.insert(Artifact.create(1L, "aa", T.now, T.now))
        val a2 = artifacts.insert(Artifact.create(1L, "bb", T.now, T.now))
        val a3 = artifacts.insert(Artifact.create(1L, "cc", T.now, T.now))
        val a4 = artifacts.insert(Artifact.create(1L, "dd", T.now, T.now))
        val a5 = artifacts.insert(Artifact.create(1L, "ee", T.now, T.now))

        clones.delete(from(clones)(c => select(c)))
        clones.insert(Clone.fake(a3.id, 2L, CloneState.awaiting, new Timestamp(600000), T.yesterday))
        clones.insert(Clone.fake(a2.id, 2L, CloneState.cloning, new Timestamp(700000), T.yesterday))
        clones.insert(Clone.fake(a5.id, 2L, CloneState.cloned, new Timestamp(700000), T.ago(3 * 24 * 60 * 60 * 1000)))
        clones.insert(Clone.fake(a4.id, 2L, CloneState.awaiting, new Timestamp(610000), T.yesterday))
        clones.insert(Clone.fake(a1.id, 2L, CloneState.cloned, new Timestamp(800000), T.ago(4 * 24 * 60 * 60 * 1000)))

        val x = new ClonedSnapshotFactory
        val xx = x.create(2)

        xx.cloned must haveSize(2)
        xx.cloned(0)._1 must be_==(a5)
        xx.cloned(1)._1 must be_==(a1)
      }
    }

  }
}
