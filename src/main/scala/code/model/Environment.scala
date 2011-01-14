package code.model

import code.gate._
import code.model.Mythos._
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._

object Environment extends LurkerComponentImpl with FileSystemComponentImpl with ClonerComponentImpl with ManipulatorComponentImpl with Loggable {
  var thresholds: List[Threshold] = Nil
  def start {
    logger.info("Environment start")
    lurker.start
    transaction(from(gateways)(g => select(g)).toSeq).foreach(watch(_))
  }
  def watch(gateway: Gateway) {
    val threshold = new Threshold(gateway, lurker, Processor)
    threshold.start
    threshold ! Maintain()
    thresholds = threshold :: thresholds
  }
  def dispose {
    logger.info("Environment end")
    lurker ! LooseInterest
    thresholds.foreach(_ ! Destroy)
    thresholds = Nil
  }
}
