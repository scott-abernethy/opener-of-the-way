package state

import _root_.util.ConcurrentUtil
import akka.actor.Actor
import model._
import concurrent.Future
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.pattern.Patterns
import concurrent.ExecutionContext.Implicits.global
import controllers.Artifacts

case class Follow(cultistId: Long)

case class Streaming(id: AnyRef, out: Enumerator[JsValue])

case class Quit(id: AnyRef)

class ArtifactStream extends Actor {

  lazy val channel = Concurrent.broadcast[JsValue]

  def receive = {
    case Follow(cid) => {
      sender ! Streaming("todo", channel._1)
    }
    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
      channel._2.push(Json.obj("type" -> change.getClass.getSimpleName, "message" -> Artifacts.artifactWithStateJson(artifact, pack.stateFor(1l))))
    }
    case other => {
      unhandled(other)
    }
  }

}

object ArtifactStream {

  lazy val stream = Environment.actorSystem.actorFor("/user/ArtifactStream")
  
  def follow(cultistId: Long): Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    Patterns.ask(stream, Follow(cultistId), 10000l).map {
      case Streaming(id, enumerator) => {
        val iteratee = Iteratee.foreach[JsValue] { event =>
          println("ignored input " + event)
        }.mapDone { _ =>
           println("quit " + id)
           stream ! Quit(id)
        }
        (iteratee, enumerator)
      }
      case other => {
        ConcurrentUtil.errorSocket("Follow failure")
      }
    }
  }
}
