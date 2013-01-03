//package util
//
//import gate.{Deactivate, Activate}
//import akka.actor.{Actor, ActorRef}
//
//case object Maintain
//
//class Maintainer(subject: ActorRef, maintainDelay: Long) extends Actor {
//  case object Pulse
//  var active = false
//
//
//  def receive = {
//    case Activate =>
//      if (!active) {
//        active = true
//        self ! Pulse
//      }
//    case Deactivate =>
//      active = false
//    case Pulse =>
//      if (active) {
//        val maintainer = self
//        Schedule.schedule(() => maintainer ! Pulse, maintainDelay)
//        subject ! Maintain
//      }
//    case msg => {
//      unhandled(msg)
//    }
//  }
//}
