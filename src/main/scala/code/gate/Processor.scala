package code.gate

import scala.collection.JavaConversions._
import java.io._

trait Processor {
  def process(processDefinition: List[String]): Processing
}

trait Cancellable {
  def cancel
}

trait Processing {
  def start(after: (Boolean, List[String]) => Unit): Cancellable
  def waitFor: (Boolean, List[String])
}

trait ProcessorComponent {
  val processor: Processor
}

trait ProcessorComponentImpl extends ProcessorComponent {
  val processor = new Processor() {
    def process(processDefinition: List[String]): Processing = new ProcessingImpl(processDefinition)
  }
}
    
private class ProcessingImpl(processDefinition: List[String]) extends Processing {
  val pb = new ProcessBuilder(processDefinition) 
  def start(after: (Boolean, List[String]) => Unit): Cancellable = {
    val t = new Thread() {
      override def run() = {
        val (s, l) = waitFor
        after(s, l)
      }
    }
    t.start
    new Cancellable() { def cancel = t.interrupt }
  }
  def waitFor: (Boolean, List[String]) = {
    var process: Process = null
    try {
      process = pb.start
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      var messages: List[String] = Nil
      var out = reader.readLine
      while (out != null) { messages = messages ::: out :: Nil ; out = reader.readLine }
      (process.waitFor == 0, ("Return code " + process.exitValue) :: messages)
    } catch {
      case e =>
        if (process != null) process.destroy
        (false, ("Exception " + e.getMessage) :: Nil)
    }
  }
}