//package gate
//
//import java.util.Calendar
//import comet._
//import model._
//import model.Mythos._
//import scala.collection.JavaConversions._
//import org.squeryl.Query
//import org.squeryl.PrimitiveTypeMode._
//import gate.Watcher._
//import akka.actor.{ActorRef, Props, Terminated, Actor}
//import play.api.Logger
//import scala.concurrent.duration._
//
//// TODO?
//
//case object Wake
//case class Warn(invalid: Clone)
//
//class Manipulator(val watcher: ActorRef, val artifactServer: ActorRef) extends Actor {
//
//  var presenter = context.system.deadLetters
//  var cloner = context.system.deadLetters
//
//  def waitingPresences(): List[Presence] = {
//    transaction(
//      sourcesQuery()
//        .toList
//        .filter(x => x._1.state == PresenceState.called && x._2.state == GateState.open)
//        .map(_._1)
//    )
//  }
//
//  def waitingClones(): List[Clone] = {
//    transaction(
//      sinksQuery()
//        .toList
//        .filter(x => x._1.state == CloneState.awaiting && x._2.state == GateState.open && x._3.map(_.state) == Some(PresenceState.present))
//        .map(_._1)
//    )
//  }
//
//  override def preStart() {
//    context.system.scheduler.schedule(60 seconds, 60 seconds, self, Wake)
//    presenter = context.actorOf(Props(new Presenter(ProcesssImpl, watcher, artifactServer)))
//    cloner = context.actorOf(Props(new Cloner(ProcesssImpl, watcher, artifactServer)))
//    self ! 'Flush
//  }
//
//  protected def receive = {
//    case Wake => {
//      // TODO waking should be a backup mechanism for doing this. Do on demand.
//      // has the current cloner timed out?
//      // get random(?) waiting clone
//      // TODO is this actually in another thread?
//      // TODO don't present if no space available!! get summoner to ensure, message goes via them.
//      // TODO one (presenter|cloner) per gateway. Can't present and clone from same gateway. Mutliple gateways may be clone dest or present source simultaneously.
//      waitingPresences() match {
//        case p :: ps => {
//          Logger.debug("Manipulator WAITING presences: " + p + " and " + ps.size + " more.")
//          // TODO
////          if (presenter.currently.isEmpty) {
////            presenter.start(p)
////          }
//        }
//        case Nil =>
//      }
//      waitingClones() match {
//        case c :: cs => {
//          Logger.debug("Manipulator WAITING clones: " + c + " and " + cs.size + " more.")
//          // TODO
////          if (cloner.currently.isEmpty) {
////            cloner.start(c)
////          }
//        }
//        case Nil =>
//      }
//    }
//    case Warn(invalid) => {
//      // TODO
//      //if (cloner.currently.filter(_ == invalid).isDefined) cloner.cancel
//    }
//    case Ping => {
//      sender ! Pong
//    }
//    case 'Flush => {
//      // TODO
////      if (cloner.currently.isEmpty) {
////        transaction ( update(clones)(c =>
////          where(c.state === CloneState.cloning)
////          set(c.state := CloneState.awaiting))
////        )
////      }
//    }
//    case FinishedCloning(clone) => {
//
//    }
//    case FinishedPresenting(presence) => {
//
//    }
//    case Terminated(ref) => {
//      // TODO
//    }
//    case msg => {
//      unhandled(msg)
//    }
//  }
//
//  override def postStop() {
//    // TODO
////    maintainer ! Destroy
////    presenter.cancel
////    cloner.cancel
//  }
//}