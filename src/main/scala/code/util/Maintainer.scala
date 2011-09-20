package code.util

import actors.Actor
import actors.Actor._
import code.gate.{Deactivate, Activate, Destroy}
import net.liftweb.util.{Schedule}

case object Maintain

class Maintainer(subject: Actor, maintainDelay: Long) extends Actor {
  case object Pulse
  var active = false
  def act() {
    loop {
      react {
        case Activate =>
          if (!active) {
            active = true
            self ! Pulse
          }
        case Deactivate =>
          active = false
        case Pulse =>
          if (active) {
            val maintainer = self
            Schedule.schedule(() => maintainer ! Pulse, maintainDelay)
            subject ! Maintain
          }
        case Destroy =>
          exit
        case _ =>
      }
    }
  }
}
