package code.gate

import code.model._
import scala.collection.JavaConversions._

class Threshold(gateway: Gateway) {
  /*
  Create two scripts for open and test, that return error codes properly
  */
  def open: Boolean = {
    try {
      val pb = new ProcessBuilder("threshold" :: "open" :: gateway.location.is :: gateway.path.is :: gateway.password.is :: Nil) 
      pb.start.waitFor == 0
    } catch {
      case io: java.io.IOException => false
      case _ => false
    }
  }
}
