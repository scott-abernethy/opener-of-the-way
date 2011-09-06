package code.model

import code.gate._
import code.model.Mythos._
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._

object Environment
  extends LurkerComponentImpl
  with FileSystemComponentImpl
  with ClonerComponentImpl
  with ManipulatorComponentImpl
  with ProcessorComponentImpl
  with Loggable
{
  var thresholds: List[Threshold] = Nil
  def start {
    logger.info("Environment start")
    lurker.start
    manipulator.start ! Activate
    transaction(from(gateways)(g => select(g)).toList).foreach(watch(_))
  }
  def watch(gateway: Gateway) {
    logger.debug("Watch " + gateway)
    val threshold = new Threshold(gateway, lurker, processor)
    threshold.start ! Activate
    thresholds = threshold :: thresholds
    logger.info(thresholds)
  }
  def dispose {
    logger.info("Environment end")
    lurker ! LooseInterest
    manipulator ! Withdraw
    thresholds.foreach(_ ! Destroy)
    thresholds = Nil
  }
}
