package code.util

import actors.Actor
import actors.Actor._
import net.liftweb.util.ActorPing
import code.gate.{Deactivate, Activate, Destroy}

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
            ActorPing.schedule(() => maintainer ! Pulse, maintainDelay)
            subject ! Maintain
          }
        case Destroy =>
          exit
        case _ =>
      }
    }
  }
}
