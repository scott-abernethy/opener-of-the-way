package code.gate

import scala.collection.JavaConversions._
import java.io._

trait Processor {
  def waitFor(processDefinition: List[String]): (Boolean, List[String])
}

object Processor extends Processor {
  def waitFor(processDefinition: List[String]): (Boolean, List[String]) = {
    try {
      val pb = new ProcessBuilder(processDefinition) 
      val process = pb.start
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      var out = reader.readLine
      while (out != null) { println(">>> " + out) ; out = reader.readLine }
      (process.waitFor == 0, Nil)
    } catch {
      case _ => (false, Nil)
    }
  }
}
