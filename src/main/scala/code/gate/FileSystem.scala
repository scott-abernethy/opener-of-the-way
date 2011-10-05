package code.gate

import scala.collection.JavaConversions._
import java.io._

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
        f.listFiles.toSeq.flatMap(find(_))      
      } else if (f isFile) { 
        f :: Nil
      } else {
        Nil
      }
    }
  }
}
