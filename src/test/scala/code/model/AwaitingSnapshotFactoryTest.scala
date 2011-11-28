package code.model

import org.specs.Specification
import org.specs.mock.Mockito
import code.TestDb
import code.model.Mythos._
import code.gate.T
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import net.liftweb.http.js.JsCmds
import xml.NodeSeq

object AwaitingSnapshotFactoryTest extends Specification with Mockito {
  val db = new TestDb
  db.init

  doBeforeSpec {
    db.reset
    inTransaction{
      val time1 = T.at(2011, 3, 20, 1, 2, 3)
      val time2 = T.at(2011, 3, 22, 1, 2, 3)
      artifacts.delete(from(artifacts)(a => select(a)))
      artifacts.insert(Artifact.create(1L, "a/b/c", time1, T.now))
      artifacts.insert(Artifact.create(2L, "fudge", time1, T.now))
      artifacts.insert(Artifact.create(1L, "d/e/f", time2, T.now))
      artifacts.insert(Artifact.create(2L, "sugar", time2, T.now))
      artifacts.insert(Artifact.create(2L, "chocolate", time2, T.now))
    }
  }

  "AwaitingSnapshot" should {
    "add awaiting and cloning items" >> {
      val a1 = new Artifact()
      a1.id = 3
      a1.gatewayId = 1
      a1.path = "/foo"
      a1.length = 9999L

      val c1 = new Clone()
      c1.id = 33
      c1.artifactId = a1.id
      c1.forCultistId = 1
      c1.state = CloneState.awaiting

      val x = new AwaitingSnapshot(Nil)
      x.awaiting must haveSize(0)

      val (snapshot, action) = x.update(a1, Some(ArtifactState.awaiting), Some(c1))

      x.awaiting must haveSize(0)
      snapshot.awaiting must haveSize(1)
      snapshot.awaiting(0) must be_==( (a1, Some(ArtifactState.awaiting), c1) )
      action must be_==( Add(c1.id) )

      val a2 = new Artifact()
      a2.id = 67
      a2.gatewayId=1
      a2.path = "/bar"
      a2.length = 9876531L

      val c2 = new Clone()
      c2.id = 89
      c2.artifactId = a2.id
      c2.forCultistId = 1
      c2.state = CloneState.cloning

      val (snapshot2, action2) = snapshot.update(a2, Some(ArtifactState.cloning), Some(c2))

      snapshot.awaiting must haveSize(1)
      snapshot2.awaiting must haveSize(2)
      snapshot2.awaiting(0) must be_==( (a1, Some(ArtifactState.awaiting), c1) )
      snapshot2.awaiting(1) must be_==( (a2, Some(ArtifactState.cloning), c2) )
      action2 must be_==( Add(c2.id) )
    }

    "update artifact state" >> {
      val a1 = new Artifact()
      a1.id = 3
      a1.gatewayId = 1
      a1.path = "/foo"
      a1.length = 9999L

      val a1prime = new Artifact()
      a1prime.id = 3
      a1prime.gatewayId = 1
      a1prime.path = "/foo"
      a1prime.length = 11L

      val c1 = new Clone()
      c1.id = 8
      c1.artifactId = a1.id
      c1.forCultistId = 1
      c1.state = CloneState.awaiting
      c1.requested = T.ago(90000L)

      val c1prime = new Clone()
      c1prime.id = 8
      c1prime.artifactId = a1.id
      c1prime.forCultistId = 1
      c1prime.state = CloneState.cloning
      c1prime.requested = T.ago(90000L)

      val a2 = new Artifact()
      a2.id = 67
      a2.gatewayId=1
      a2.path = "/bar"
      a2.length = 9876531L

      val c2 = new Clone()
      c2.id = 10
      c2.artifactId = a2.id
      c2.forCultistId = 1
      c2.state = CloneState.cloning
      c2.requested = T.ago(20000L)

      val x = new AwaitingSnapshot( (a1, Some(ArtifactState.awaiting), c1) :: (a2, Some(ArtifactState.cloning), c2) :: Nil )
      x.awaiting must haveSize(2)

      val (snapshot, action) = x.update(a1prime, Some(ArtifactState.cloning), Some(c1prime))

      snapshot.awaiting must haveSize(2)
      snapshot.awaiting(0) must be_==( (a1prime, Some(ArtifactState.cloning), c1prime) )
      snapshot.awaiting(1) must be_==( (a2, Some(ArtifactState.cloning), c2) )
      action must be_==( Update(c1prime.id) )
    }

    "remove cloned artifacts" >> {
      val a1 = new Artifact()
      a1.id = 3
      a1.gatewayId = 1
      a1.path = "/foo"
      a1.length = 9999L

      val c1 = new Clone()
      c1.id = 98
      c1.artifactId = a1.id
      c1.forCultistId = 1
      c1.state = CloneState.awaiting
      c1.requested = T.ago(90000L)

      val a2 = new Artifact()
      a2.id = 67
      a2.gatewayId=1
      a2.path = "/bar"
      a2.length = 9876531L

      val a2prime = new Artifact()
      a2prime.id = a2.id
      a2prime.gatewayId = 1
      a2prime.path = "/bar"
      a2prime.length = 9876531L

      val c2 = new Clone()
      c2.id = 12
      c2.artifactId = a2.id
      c2.forCultistId = 1
      c2.state = CloneState.cloning
      c2.requested = T.ago(20000L)

      val c2prime = new Clone()
      c2prime.id = 12
      c2prime.artifactId = a2.id
      c2prime.forCultistId = 1
      c2prime.state = CloneState.cloned
      c2prime.requested = T.ago(90000L)
      
      val x = new AwaitingSnapshot( (a1, Some(ArtifactState.awaiting), c1) :: (a2, Some(ArtifactState.cloning), c2) :: Nil )
      x.awaiting must haveSize(2)

      val (snapshot, action) = x.update(a2prime, Some(ArtifactState.cloned), Some(c2prime))
      snapshot.awaiting must haveSize(1)
      snapshot.awaiting(0) must be_==( (a1, Some(ArtifactState.awaiting), c1) )
      action must be_==( Remove(c2prime.id) )
    }
  }

  "AwaitingSnapshotFactory" should {

    "be empty for no clones" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new AwaitingSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must beEmpty
    }

    "only show artifacts cloned by cultist" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloning)))
      val x = new AwaitingSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must haveSize(1)
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

      val x = new AwaitingSnapshotFactory
      val xx = x.create(2)
      xx.awaiting must haveSize(0)
    }

    "ordered by non-completes by request date, then completes by completion date" >> {
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

        val x = new AwaitingSnapshotFactory
        val xx = x.create(2)
        xx.awaiting must haveSize(3)
        xx.awaiting(0)._1 must be_==(a2)
        xx.awaiting(1)._1 must be_==(a4)
        xx.awaiting(2)._1 must be_==(a3)
      }
    }

    "state of single artifact" >> {
      inTransaction {
        val a1 = artifacts.insert(Artifact.create(1L, "yuiiuy", T.now, T.now))
        val x = new AwaitingSnapshotFactory
        x.stateOf(2, a1.id) must beSome( (a1, Some(ArtifactState.glimpsed), None) )
        x.stateOf(2, a1.id + 1) must beNone

        presences.insert(Presence.create(a1.id, PresenceState.present))
        x.stateOf(2, a1.id) must beSome( (a1, Some(ArtifactState.present), None) )

        val c1 = clones.insert(Clone.fake(a1.id, 2L, CloneState.awaiting, T.now, T.yesterday))
        x.stateOf(2, a1.id) must beSome( (a1, Some(ArtifactState.awaitingPresent), Some(c1)) )

        clones.insert(Clone.fake(a1.id, 1L, CloneState.cloned, T.yesterday, T.yesterday))
        x.stateOf(2, a1.id) must beSome( (a1, Some(ArtifactState.awaitingPresent), Some(c1)) )

        clones.deleteWhere(c => c.id === c1.id)
        x.stateOf(2, a1.id) must beSome( (a1, Some(ArtifactState.present), None) )
      }
    }

  }

  doAfter {
    db.close
  }
}
