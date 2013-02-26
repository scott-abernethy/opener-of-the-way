package util

import concurrent.ExecutionContext
import model.Environment

object Context {
  implicit val playDefault: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val defaultOperations: ExecutionContext = Environment.actorSystem.dispatchers.defaultGlobalDispatcher
  implicit val dbOperations: ExecutionContext = Environment.actorSystem.dispatchers.lookup("akka.actor.db-dispatcher")
  implicit val ioOperations: ExecutionContext = Environment.actorSystem.dispatchers.lookup("akka.actor.io-dispatcher")
}