package code.model

import org.specs.runner.{ConsoleRunner, JUnit4}
import org.specs.Specification
import org.specs.mock.Mockito
import code.TestDb
import code.model.Mythos._
import code.gate.T
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp

class ArtifactCloneSnapshotTestSpecsAsTest extends JUnit4(ArtifactCloneSnapshotTestSpecs)
object ArtifactCloneSnapshotTestSpecsRunner extends ConsoleRunner(ArtifactCloneSnapshotTestSpecs)
object ArtifactCloneSnapshotTestSpecs extends Specification with Mockito {
  doBeforeSpec {
    TestDb.init
    inTransaction{
      val time1 = new Timestamp(111, 3, 20, 1, 2, 3, 0)
      val time2 = new Timestamp(111, 3, 22, 1, 2, 3, 0)
      artifacts.delete(from(artifacts)(a => select(a)))
      artifacts.insert(new Artifact(1L, "a/b/c", time1, T.now))
      artifacts.insert(new Artifact(2L, "fudge", time1, T.now))
      artifacts.insert(new Artifact(1L, "d/e/f", time2, T.now))
      artifacts.insert(new Artifact(2L, "sugar", time2, T.now))
      artifacts.insert(new Artifact(2L, "chocolate", time2, T.now))
    }
  }
  "ArtifactCloneSnapshot" should {
    "load all artifacts" >> {
      val x = new ArtifactCloneSnapshot
      x.reload(1)
      x.states.size mustEqual(5)

      x.reload(2)
      x.states.size mustEqual(5)      
    }
    "group artifacts by discovery, order by path" >> {
      val x = new ArtifactCloneSnapshot
      x.reload(1)
      x.items.keys.toSeq mustEqual("2011-04-20, Wednesday" :: "2011-04-22, Friday" :: Nil)
      x.items.get("2011-04-20, Wednesday").map(as => as.map(a => a.path)) must beSome("a/b/c" :: "fudge" :: Nil)
    }
    "default state to mine or available if no clones exist" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new ArtifactCloneSnapshot
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactState.available)
      x.states.get(i + 1) must beSome(ArtifactState.mine)
      x.states.get(i + 2) must beSome(ArtifactState.available)
      x.states.get(i + 3) must beSome(ArtifactState.mine)
      x.states.get(i + 4) must beSome(ArtifactState.mine)
      x.states.get(i + 5) must beNone
    }
    "reflect clone state" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.queued)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.progressing)))
      val x = new ArtifactCloneSnapshot
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.queued)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.mine)

      myClone.state = CloneState.progressing
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.progressing)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.mine)

      myClone.state = CloneState.done
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactState.done)
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactState.mine)
    }
    "allow artifacts to be updated" >> {
      inTransaction(clones.delete(from(clones)(c => select(c))))
      val x = new ArtifactCloneSnapshot
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactState.available)

      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.queued)))
      x.update(i)
      x.states.get(i) must beSome(ArtifactState.queued)      
    }
  }
  doAfterSpec {
    TestDb.close
  }
}
