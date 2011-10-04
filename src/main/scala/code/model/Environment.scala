package code.model

import code.gate._
import code.model.Mythos._
import net.liftweb.common._
import org.squeryl.PrimitiveTypeMode._
import akka.actor.Actor.actorOf
import akka.actor.{ActorRef, Scheduler}
import java.util.concurrent.TimeUnit

object Environment
  extends LurkerComponentImpl
  with FileSystemComponentImpl
  with ClonerComponentImpl
  with PresenterComponentImpl
  with ManipulatorComponentImpl
  with ProcessorComponentImpl
  with Loggable
{
  var threshold: ActorRef = _
  var watcher: ActorRef = _

  def start {
    logger.info("Environment start")

    threshold = actorOf(new Threshold(processor)).start
    watcher = actorOf(new Watcher(threshold, lurker)).start
    Scheduler.schedule(watcher, 'Wake, 1, 5, TimeUnit.MINUTES)
    lurker.start
    lurker ! 'Flush
    manipulator.start
    manipulator ! 'Flush
    manipulator ! Activate
  }

  def dispose {
    logger.info("Environment end")
    lurker ! LooseInterest
    manipulator ! Withdraw
    threshold.stop()
    watcher.stop()
  }
}
