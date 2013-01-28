package state

import akka.actor.{ActorRef, Actor}

case class Babble(who: String, text: String)

class BabbleServer extends Actor {

  lazy val stream: ActorRef = context.system.actorFor("/user/StateStream")
  var items: List[Babble] = List(Babble("???", "Service restarted"))

  def receive = {
    case 'List => {
      sender ! items
    }
    case in @ Babble(who, text) if (text.trim.size > 0) => {
      items = in :: items.take(10)
      stream ! in
    }
    case other => {
      unhandled(other)
    }
  }
}