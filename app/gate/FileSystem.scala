/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
