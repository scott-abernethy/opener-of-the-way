/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package state

import _root_.util.{DatePresentation, ConcurrentUtil}
import akka.actor.{Terminated, ActorRef, Actor, Props}
import model._
import concurrent.Future
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.pattern.Patterns
import controllers.Artifacts
import play.api.Logger
import concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import java.util.Date

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

      import _root_.util.Context.defaultOperations
      val streamingFuture = Patterns.ask(ref, Stream(id), 10000L).map {
        case CultistStreamReady(channel) => Streaming(id, channel)
      }

      Patterns.pipe(streamingFuture, context.dispatcher).to(sender)
    }
    case pack @ ArtifactPack(change, _, ownerId, _, _) => {
      change match {
        case ArtifactCreated => {
          broadcast(artifactCreatedMsg(pack))
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
    case b: Babble => {
      broadcast(_ => Json.obj("type" -> "BabbleAdd", "message" -> b.toJson))
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
  }

  def unicast(cultistId: Long, msg: Long => Any) {
    streams.get(cultistId).foreach(_ ! msg(cultistId))
  }

  def broadcast(msg: Long => Any) {
    streams.foreach{ case (cid, ref) => ref ! msg(cid) }
  }

  def artifactCreatedMsg(pack: ArtifactPack)(cid: Long): JsObject = {
    val group = DatePresentation.yearMonthDay(pack.artifact.discovered.getTime)
    Json.obj(
      "type" -> "ArtifactCreated",
      "message" -> Json.obj(
        "group" -> group,
        "artifact" -> Artifacts.artifactWithStateJson(pack.artifact, pack.stateFor(cid), pack.cloneFor(cid).map(_.attempts), pack.cloneCount())
      )
    )
  }

  def artifactMsg(msgType: String, pack: ArtifactPack)(cid: Long): JsObject = {
    Json.obj(
      "type" -> msgType,
      "message" -> Artifacts.artifactWithStateJson(pack.artifact, pack.stateFor(cid), pack.cloneFor(cid).map(_.attempts), pack.cloneCount())
    )
  }

  def gatewaysMsg(cid: Long): JsObject = {
    Json.obj(
      "type" -> "GatewayReload",
      "message" -> Json.toJson(Gateway.forCultist(cid).map(_.toJson))
    )
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
      context.system.scheduler.scheduleOnce(FiniteDuration.apply(60, TimeUnit.SECONDS), self, 'Quit)(_root_.util.Context.defaultOperations)
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
  }
}

object StateStream {

  lazy val stream = Environment.actorSystem.actorFor("/user/StateStream")
  
  def follow(cultistId: Long): Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    import _root_.util.Context.defaultOperations
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
