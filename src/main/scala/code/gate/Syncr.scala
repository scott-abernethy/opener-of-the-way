package code.gate

import scala.collection.JavaConversions._

object Syncr {
  def go {
    val pb = new ProcessBuilder("rsync" :: Nil) 
    /*
    -q quiet
    -t preserve modification times
    --progress
    */
  }
}
