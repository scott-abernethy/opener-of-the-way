package code.gate

import code.model._
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._

case class WayFound(gateway: Gateway, localPath: String)
case class WayLost(gateway: Gateway)

class Lurker extends Actor {
  def act() {
    while (true) {
      receive {
        case WayFound(g, lp) =>
          // save state
          // if recently opened parse it (see contract)
        case WayLost(g) =>
          // save state
        case _ =>
      }
    }
  }
}
