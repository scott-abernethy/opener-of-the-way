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

package gate

import akka.actor.{Props, ActorRef, Actor}

object KeeperRouterApi {
  case class ToKeeper(gatewayId: Long, msg: AnyRef)
  case class ToAll(msg: AnyRef)
}

/**
 * Merge with GatewayServer
 */
class KeeperRouter(artifactServer: ActorRef) extends Actor {
  import KeeperRouterApi._

  lazy val watcher = context.system.actorFor("/user/Watcher")
  var keepers = Map.empty[Long, ActorRef]
  lazy val locker = context.system.deadLetters

  def receive = {
    case ToKeeper(gatewayId, msg) => {
      keeperFor(gatewayId) ! msg
    }
    case ToAll(msg) => {
      keepers.values.foreach(_ ! msg)
    }
    case msg => {
      unhandled(msg)
    }
  }

  def keeperFor(gatewayId: Long): ActorRef = {
    def addKeeper(gatewayId: Long): ActorRef = {
      val ref = context.actorOf(Props(new Keeper(gatewayId, locker, ProcesssFactory.create, watcher, artifactServer)), "KeeperOf" + gatewayId)
      keepers = keepers + (gatewayId -> ref)
      ref
    }
    keepers.get(gatewayId).getOrElse(addKeeper(gatewayId))
  }
}