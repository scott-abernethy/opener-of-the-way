package code.gate

import scala.collection.JavaConversions._
import java.io._
import net.liftweb.util.Helpers._

trait FileSystem {
  def find(path: String): Seq[(String, Long)]
}
trait FileSystemComponent {
  val fileSystem: FileSystem
}

trait FileSystemComponentImpl extends FileSystemComponent {
  val fileSystem = new FileSystem {

    def find(path: String): Seq[(String,Long)] = {
      for {
        f <- find(new File(path))
        relativePath = f.getPath.substring(path.length)
      } yield (relativePath, f.length)
    }

    def find(f: File): Seq[File] = {
      if (f isDirectory) {
        for {
          child <- tryo(f.listFiles.toSeq) getOrElse Nil
          found <- tryo(find(child)) getOrElse Nil
        } yield found
      } else if (f isFile) { 
        f :: Nil
      } else {
        Nil
      }
    }
  }
}
