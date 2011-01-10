package code.model

import code.gate._
import code.model.Mythos._
import net.liftweb.common._
import net.liftweb.squerylrecord.RecordTypeMode._

object Environment extends LurkerComponentImpl with FileSystemComponentImpl with Loggable {
  var thresholds: List[Threshold] = Nil
  def start {
    logger.info("Environment start")
    lurker.start
    Db.use(_ => from(gateways)(g => select(g)).toSeq).foreach(watch(_))
    logger.info("Environment started")
  }
  def watch(gateway: Gateway) {
    logger.info("Watch " + gateway)
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
