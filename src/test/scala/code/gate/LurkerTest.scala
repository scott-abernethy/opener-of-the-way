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
        fileSystem.find("/srv/g") returns(("folder/file", 345L) :: ("folder/sub/another-file.txt", 12L) :: Nil)
        var g = queryG(1L)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as = transaction { g.artifacts toList }
        as must haveSize(2)
        as(0).path must be_==("folder/file")
        as(0).length must be_==(345L)
        as(1).path must be_==("folder/sub/another-file.txt")
        as(1).length must be_==(12L)

        transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
        fileSystem.find("/srv/g") returns(("folder/file", 345L) :: ("folder/sub/another-file.txt", 7654321L) :: ("readme.nfo", 7777L) :: Nil)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as2 = transaction { g.artifacts toList }
        as2 must haveSize(3)
        as2(0).path must be_==("folder/file")
        as2(0).length must be_==(345L)
        as2(1).path must be_==("folder/sub/another-file.txt")
        as2(1).length must be_==(7654321L)
        as2(2).path must be_==("readme.nfo")
        as2(2).length must be_==(7777L)
      }

      "ignore silly files" >> {
        transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
        transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
        fileSystem.find("/srv/g") returns(
          ("/System Volume Information/xx", 1L) ::
          ("System Volume Information/xy", 2L) ::
          ("/Recycled/yx", 1L) ::
          ("Recycled/yy", 4L) ::
          ("/folder/sub/another-file.txt", 7654L) ::
          ("/ignored/stuff/foo.txt", 14L) ::
          ("/ignored/stuff/foo.txt", 14L) ::
          ("/$RECYCLE.BIN/$IUOSDFY.xxu", 34L) ::
          ("$RECYCLE.BIN/uiopuip", 34L) ::
          ("$other", 34L) ::
          ("/.hidden", 34L) ::
          ("/.gtk/settings", 1L) ::
          ("/.Trash-52342343/foobarbaz.qux", 151412L) ::
          Nil
        )
        var g = queryG(1L)
        x ! WayFound(g, "/srv/g")
        x !? (5000, Ping)
        val as = transaction { g.artifacts toList }
        as must haveSize(1)
        as(0).path must be_==("/folder/sub/another-file.txt")
      }
      //"removing missing artifacts" >> {}
      //"cancelling bad copy jobs" >> {}
    }

    "ignore artifacts on sink gates" >> {
      transaction { Mythos.artifacts.delete(from(Mythos.artifacts)(a => select(a))) }
      transaction { update(Mythos.gateways)(g => setAll(g.scoured := T.yesterday)) }
      fileSystem.find("/srv/g") returns(("folder/file", 88L) :: ("folder/sub/another-file.txt", 999L) :: Nil)
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
