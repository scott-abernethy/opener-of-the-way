package code.gate

import scala.collection.JavaConversions._
import java.io._

case class Result(success: Boolean, messages: List[String], duration: Long)

trait Processor {
  def process(processDefinition: List[String]): Processing
}

trait Cancellable {
  def cancel
}

trait Processing {
  def start(after: Result => Unit): Cancellable
  def waitFor: Result
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
  def start(after: Result => Unit): Cancellable = {
    val t = new Thread() {
      override def run() = {
        after(waitFor)
      }
    }
    t.start
    new Cancellable() { def cancel = t.interrupt }
  }
  def waitFor: Result = {
    val startedAt = System.currentTimeMillis
    var process: Process = null
    try {
      process = pb.start
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      var messages: List[String] = Nil
      var out = reader.readLine
      while (out != null) { messages = messages ::: out :: Nil ; out = reader.readLine }
      Result(process.waitFor == 0, ("Return code " + process.exitValue) :: messages, System.currentTimeMillis - startedAt)
    } catch {
      case e =>
        if (process != null) process.destroy
        Result(false, ("Exception " + e.getMessage) :: Nil, -1)
    }
  }
}