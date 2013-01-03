package gate

import akka.actor.{Props, ActorRef, Actor}
import model.{Clone, Presence}

case class Admit(xs: Seq[Presence])
case class Release(xs: Seq[Clone])
case class Cancel(xs: Seq[Clone])

/*
keeps track of in out for this 'node' .. being a unique gateway location
clones and presences
 */

class Keeper(val processs: Processs, val watcher: ActorRef, val artifactServer: ActorRef) extends Actor {
  var queuedPresences: Seq[Presence] = Nil
  var queuedClones: Seq[Clone] = Nil

  def receive = {
    case Admit(xs) => {
      queuedPresences = (queuedPresences ++ xs).distinct
      check()
    }
    case Release(xs) => {
      queuedClones = (queuedClones ++ xs).distinct
      check()
    }
    case Cancel(xs) => {
      queuedClones = queuedClones filterNot (xs.toSet.contains)
      for (child <- context.children; job <- xs) child ! CancelCloning(job)
    }
    case FinishedCloning(clone) => {
      // probably need to wait, or death watch
      check()
    }
    case msg => {
      unhandled(msg)
    }
  }

  def check() {
    if (context.children.isEmpty) {
      start(queuedPresences.headOption orElse queuedClones.headOption)
    }
  }

  def start(item: AnyRef) {
    item match {
      case p: Presence => {
        val presenter: ActorRef = context.actorOf(Props(new Presenter(processs, watcher, artifactServer)))
        presenter ! StartPresenting(p)
      }
      case c: Clone => {
        val cloner: ActorRef = context.actorOf(Props(new Cloner(processs, watcher, artifactServer)))
        cloner ! StartCloning(c)
      }
      case _ => {}
    }
  }
}