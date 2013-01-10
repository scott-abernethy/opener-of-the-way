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
      val ref = context.actorOf(Props(new Keeper(gatewayId, locker, ProcesssImpl, watcher, artifactServer)), "KeeperOf" + gatewayId)
      keepers = keepers + (gatewayId -> ref)
      ref
    }
    keepers.get(gatewayId).getOrElse(addKeeper(gatewayId))
  }
}