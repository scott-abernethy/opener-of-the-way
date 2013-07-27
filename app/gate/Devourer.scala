package gate

import akka.actor.Actor
import util.Context
import util.FutureTransaction._
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import model._
import play.api.Logger
import scala.concurrent.Future
import akka.actor.ActorRef

/**
 * When woken, the Devourer will purge all old history.
 */
class Devourer extends Actor {
  
  var tasks: List[Symbol] = List()
  var waker: ActorRef = context.system.deadLetters
  
  def receive = {
    case 'Wake => {
      Logger.info("Devourer woken")
      
      if (tasks.size > 0) {
        Logger.warn("Devourer did not finish on last run!")
      }
      
      waker = sender
      tasks = List('PurgeClones, 'PurgeBabbles, 'PurgeArtifacts)
      
      self ! 'Next
    }
    case 'Next => {
      // Do the next task or sleep
      tasks match {
        case head :: tail => {
          tasks = tail
          self ! head
        }
        case Nil => {
          Logger.info("Devourer sleeping")
          waker ! 'DevourerSleepy
          waker = context.system.deadLetters
        }
      }
    }
    case 'PurgeClones => {
      performTask( futureTransaction{
        clones.deleteWhere(c => (c.state === CloneState.cloned) and (c.attempted < T.ago(Clone.purgeAfter)))
      })
    }
    case 'PurgeBabbles => {
      performTask( futureTransaction{
        babbles.deleteWhere(b => b.at < T.ago(Babble.purgeAfter))
      })
    }
    case 'PurgeArtifacts => {
      performTask( futureTransaction{
        val purgable = from(artifacts)(a => 
          where(a.witnessed < T.ago(Artifact.purgeAfter) and a.discovered < T.ago(Artifact.purgeAfter)) 
          select(a.id)
          ).toList
        update(presences)(p => where(p.artifactId in purgable) set(p.state := PresenceState.released))
        clones.deleteWhere(c => c.artifactId in purgable)
        
        val deletable = join(artifacts, presences.leftOuter)( (a,p) => 
          where(a.witnessed < T.ago(Artifact.purgeAfter) and a.discovered < T.ago(Artifact.purgeAfter) and p.map(_.state).~.isNull) 
          select(a.id)
          on(a.id === p.map(_.artifactId))
          ).toList
        artifacts.deleteWhere(a => a.id in deletable)
      })
    }
    case other => {
      unhandled(other)
    }
  }
  
  def performTask[A](task: Future[A]) {
    val listener = self
    task.onComplete(x => listener ! 'Next)(Context.defaultOperations)
  }
}