package code.gate

import scala.collection.JavaConversions._
import java.io._

trait FileSystem {
  def find(path: String): Seq[String]
}

object FileSystem extends FileSystem {
  def find(path: String): Seq[String] = find(new File(path)) map(_.getPath.substring(path.length))
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
