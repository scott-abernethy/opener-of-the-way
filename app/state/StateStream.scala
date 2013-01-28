package state

import _root_.util.ConcurrentUtil
import akka.actor.{Terminated, ActorRef, Actor, Props}
import model._
import concurrent.Future
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.pattern.Patterns
import concurrent.ExecutionContext.Implicits.global
import controllers.Artifacts
import play.api.Logger
import comet.{FlushAllGateways, ChangedGateway, ChangedGateways, ToState}
import concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

case class Follow(cultistId: Long)
case class Stream(id: Long)

case class Streaming(id: Long, out: Enumerator[JsValue])

case class Quit(cultistId: Long, id: Long)
case class StopStream(id: Long)

class StateStream extends Actor {

  var nextId: Long = 0L
  var streams = Map.empty[Long, ActorRef]


  override def preStart() {
    super.preStart()
    Logger.debug(self + " started")
  }

  def receive = {
    case Follow(cid) => {
      val id = nextId
      nextId = nextId + 1
      val ref = streams.get(cid).getOrElse {
        val x = context.actorOf(Props[CultistStream])
        context.watch(x)
        streams = streams + (cid -> x)
        x
      }

      // There is a chance that the follow request comes in exactly 10 seconds later, as the stream quits but before termination notification has been received.

      val streamingFuture = Patterns.ask(ref, Stream(id), 10000L).map {
        case CultistStreamReady(channel) => Streaming(id, channel)
      }

      Patterns.pipe(streamingFuture, context.dispatcher).to(sender)
    }
    case pack @ ArtifactPack(change, _, ownerId, _, _) => {
      change match {
        case ArtifactCreated => {
          broadcast(artifactMsg("ArtifactCreated", pack))
        }
        case ArtifactAwaiting(forCultistId) => {
          unicast(ownerId, artifactMsg("ArtifactUpdate", pack))
          unicast(forCultistId, artifactMsg("ArtifactAwaiting", pack))
        }
        case ArtifactUnawaiting(forCultistId) => {
          unicast(ownerId, artifactMsg("ArtifactUpdate", pack))
          unicast(forCultistId, artifactMsg("ArtifactUnawaiting", pack))
        }
        case ArtifactPresented => {
          broadcast(artifactMsg("ArtifactUpdate", pack))
        }
        case ArtifactCloning(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloning", pack))
        }
        case ArtifactCloneFailed(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloneFailed", pack))
        }
        case ArtifactCloned(forCultistId) => {
          unicast(forCultistId, artifactMsg("ArtifactCloned", pack))
        }
        case ignored => {}
      }
    }
    case FlushAllGateways => {
      broadcast(gatewaysMsg)
    }
    case ToState(_, _, cultistId) => {
      unicast(cultistId, gatewaysMsg)
    }
    case ChangedGateway(_, cultistId) => {
      unicast(cultistId, gatewaysMsg)
    }
    case ChangedGateways(cultistId) => {
      unicast(cultistId, gatewaysMsg)
    }
    case Babble(who, text) => {
      broadcast(_ => Json.obj("type" -> "BabbleAdd", "message" -> Json.obj("who" -> who, "text" -> text)))
    }
    case Quit(cid, id) => {
      streams.get(cid) match {
        case Some(ref) => {
          ref ! StopStream(id)
        }
        case _ => {}
      }
    }
    case Terminated(t) => {
      Logger.debug("Terminated " + t)
      streams = streams.filter( x => !t.equals(x._2) )
    }
    case other => {
      unhandled(other)
    }
  }

  def unicast(cultistId: Long, msg: Long => Any) {
    streams.get(cultistId).foreach(_ ! msg(cultistId))
  }

  def broadcast(msg: Long => Any) {
    streams.foreach{ case (cid, ref) => ref ! msg(cid) }
  }

  def artifactMsg(msgType: String, pack: ArtifactPack)(cid: Long): JsObject = {
    Json.obj("type" -> msgType, "message" -> Artifacts.artifactWithStateJson(pack.artifact, pack.stateFor(cid)))
  }

  def gatewaysMsg(cid: Long): JsObject = {
    Json.obj("type" -> "GatewayReload", "message" -> Json.toJson(Gateway.forCultist(cid).map(_.toJson)))
  }

}

case class CultistStreamReady(channel: Enumerator[JsValue])

class CultistStream extends Actor {

  var followers = Set.empty[Long]
  lazy val channel = Concurrent.broadcast[JsValue]

  def receive = {
    case Stream(id) => {
      followers = followers + id
      Logger.debug("Cultist stream start " + id + " now " + followers)
      sender ! CultistStreamReady(channel._1)
    }
    case StopStream(id) => {
      followers = followers - id
      Logger.debug("Cultist stream stop " + id + " now " + followers)
      context.system.scheduler.scheduleOnce(FiniteDuration.apply(60, TimeUnit.SECONDS), self, 'Quit)
    }
    case 'Quit => {
      if (followers.isEmpty) {
        context.stop(self)
        Logger.debug("Cultist stream quit " + self)
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
          Logger.debug("Stream " + id + " input ignored " + event)
        }.mapDone { _ =>
          Logger.debug("Stream " + id + " quit")
          stream ! Quit(cultistId, id)
        }
        (iteratee, enumerator)
      }
      case other => {
        Logger.warn("Stream follow failure")
        ConcurrentUtil.errorSocket("Follow failure")
      }
    }
  }
}
