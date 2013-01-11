package state

import _root_.util.ConcurrentUtil
import akka.actor.{ActorRef, Actor, Props}
import model._
import concurrent.Future
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.pattern.Patterns
import concurrent.ExecutionContext.Implicits.global
import controllers.Artifacts
import play.Logger
import comet.{FlushAllGateways, ChangedGateway, ChangedGateways, ToState}
import concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

case class Follow(cultistId: Long)

case class Streaming(id: Long, out: Enumerator[JsValue])

case class Quit(id: Long)

class StateStream extends Actor {

  var streams = Map.empty[Long, ActorRef]

  def receive = {
    case Follow(cid) => {
      val ref = streams.get(cid).getOrElse {
        val x = context.actorOf(Props[CultistStream])
        streams = streams + (cid -> x)
        x
      }

      val streamingFuture = Patterns.ask(ref, 'Channel, 10000L).map {
        case CultistStreamReady(channel) => Streaming(cid, channel)
      }

      Patterns.pipe(streamingFuture, context.dispatcher).to(sender)
    }
    case pack @ ArtifactPack(change, _, ownerId, _, _) => {
      change match {
        case ArtifactCreated => {
          broadcast(artifactMsg("ArtifactCreated", pack))
        }
        case ArtifactAwaiting(forCultistId) => {
          unicast(ownerId, artifactMsg("ArtifactUpdate", pack)(ownerId))
          unicast(forCultistId, artifactMsg("ArtifactAwaiting", pack)(forCultistId))
        }
        case ArtifactUnawaiting(forCultistId) => {
          unicast(ownerId, artifactMsg("ArtifactUpdate", pack)(ownerId))
          unicast(forCultistId, artifactMsg("ArtifactUnawaiting", pack)(forCultistId))
        }
        case ArtifactPresented => {
          broadcast(artifactMsg("ArtifactUpdate", pack))
        }
        case ArtifactCloning(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloning", pack)(forCultistId))
        }
        case ArtifactCloneFailed(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloneFailed", pack)(forCultistId))
        }
        case ArtifactCloned(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloned", pack)(forCultistId))
        }
        case ignored => {}
      }
    }
    case FlushAllGateways => {
      broadcast(_ => 'GatewayChanged)
    }
    case ToState(_, _, cultistId) => {
      unicast(cultistId, 'GatewayChanged)
    }
    case ChangedGateway(_, cultistId) => {
      unicast(cultistId, 'GatewayChanged)
    }
    case ChangedGateways(cultistId) => {
      unicast(cultistId, 'GatewayChanged)
    }
    case Quit(cid) => {
      streams.get(cid) match {
        case Some(ref) => {
          ref ! 'QuitLater
        }
        case _ => {}
      }
    }
    case other => {
      unhandled(other)
    }
  }

  def unicast(cultistId: Long, msg: Any) {
    streams.get(cultistId).foreach(_ ! msg)
  }

  def broadcast(msg: Long => Any) {
    streams.foreach{ case (cid, ref) => ref ! msg(cid) }
  }

  def artifactMsg(msgType: String, pack: ArtifactPack)(cid: Long): JsObject = {
    Json.obj("type" -> msgType, "message" -> Artifacts.artifactWithStateJson(pack.artifact, pack.stateFor(cid)))
  }

}

case class CultistStreamReady(channel: Enumerator[JsValue])

class CultistStream extends Actor {

  var streaming = true
  lazy val channel = Concurrent.broadcast[JsValue]

  def receive = {
    case 'Channel => {
      streaming = true
      sender ! CultistStreamReady(channel._1)
    }
    case 'QuitLater => {
      streaming = false
      context.system.scheduler.scheduleOnce(FiniteDuration.apply(10, TimeUnit.SECONDS), self, 'Quit)
    }
    case 'Quit => {
      if (!streaming) {
        context.stop(self)
      }
    }
    case json: JsValue => {
      channel._2.push(json)
    }
    case other => {
      unhandled(other)
    }
  }
}

object StateStream {

  lazy val stream = Environment.actorSystem.actorFor("/user/StateStream")
  
  def follow(cultistId: Long): Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    Patterns.ask(stream, Follow(cultistId), 10000l).map {
      case Streaming(id, enumerator) => {
        val iteratee = Iteratee.foreach[JsValue] { event =>
          println("Stream input ignored " + event)
        }.mapDone { _ =>
           println("Stream quit " + id)
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
