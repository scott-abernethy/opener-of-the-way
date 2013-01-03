package gate

import java.io._
import scala.util.control.Exception._
import play.api.Logger

trait FileSystem {
  def find(path: String): Seq[(String, Long)]
}
trait FileSystemComponent {
  val fileSystem: FileSystem
}




object FileSystemImpl extends FileSystem {

    def find(path: String): Seq[(String,Long)] = {
      for {
        f <- find(new File(path))
        relativePath = f.getPath.substring(path.length)
      } yield (relativePath, f.length)
    }

    def find(f: File): Seq[File] = {
      if (f.isDirectory) {
        for {
          child <- catching(classOf[Exception]).withApply{err => Logger.warn("Oops", err); Nil}.apply(f.listFiles.toSeq)
          found <- catching(classOf[Exception]).withApply{err => Logger.warn("Oops", err); Nil}.apply(find(child))
        } yield found
      } else if (f.isFile) {
        f :: Nil
      } else {
        Nil
      }
    }
}
