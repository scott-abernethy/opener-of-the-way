package code.gate

import org.specs._
import org.specs.mock.Mockito

import code.TestDb
import code.model._

import org.squeryl.PrimitiveTypeMode._

object LurkerTest extends Specification with Mockito {
  val db = new TestDb
  db.init

  doBeforeSpec {
    db.reset
  }

  "Lurker" should {
    def queryG(id: Long): Gateway = transaction { Mythos.gateways.lookup(id) getOrElse null }
    object LurkerComponentTest extends LurkerComponentImpl with FileSystemComponent with ManipulatorComponent {
      val fileSystem = mock[FileSystem]
      val manipulator = mock[Manipulator]
    }
    val fileSystem = LurkerComponentTest.fileSystem
    fileSystem.find("/srv/f") returns(Nil)
    
    val x = LurkerComponentTest.lurker.start

    "parse artifacts on way found" >> {
      "adding new artifacts" >> {
        transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
        transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: Nil)
        var g = queryG(1L)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as = transaction { g.artifacts toList }
        as must haveSize(2)

        transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
        fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: "readme.nfo" :: Nil)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as2 = transaction { g.artifacts toList }
        as2 must haveSize(3)
      }

      "ignore silly files" >> {
        transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
        transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
        fileSystem.find("/srv/g") returns("/System Volume Information/xx" :: "System Volume Information/xy" :: "/Recycled/yx" :: "Recycled/yy" :: "folder/sub/another-file.txt" :: Nil)
        var g = queryG(1L)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as = transaction { g.artifacts toList }
        as must haveSize(1)
      }
      //"removing missing artifacts" >> {}
      //"cancelling bad copy jobs" >> {}
    }

    "ignore artifacts on sink gates" >> {
      transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
      transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
      fileSystem.find("/srv/g") returns("folder/file" :: "folder/sub/another-file.txt" :: Nil)
      var g = queryG(2L)
      x ! WayFound(g, "/srv/g")
      x !? (5000, Ping)
      val as = transaction { g.artifacts toList }
      as must haveSize(0)
    }
    //"activate outstanding copies on way found" >> {}
  }

  doAfter {
    db.close
  }
}
