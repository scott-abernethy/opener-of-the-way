package model

import gate._
import comet._
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.{Play, Logger}
import concurrent.Await
import concurrent.duration._
import state.{BabbleServer, StateStream, ArtifactServer}
import com.typesafe.config.Config

object Environment {
  var actorSystem: ActorSystem = _

  def start {
    val config: Config = Play.configuration(Play.current).underlying
    actorSystem = ActorSystem.create("YogSothoth", config.getConfig("ootw").withFallback(config))
    val artifactServer = actorSystem.actorOf(Props[ArtifactServer], "ArtifactServer")
    val keepers = actorSystem.actorOf(Props(new KeeperRouter(artifactServer)), "Keepers")
    val stateStream = actorSystem.actorOf(Props[StateStream], "StateStream")
    val lurker = actorSystem.actorOf(Props(new Lurker(actorSystem.deadLetters, artifactServer, stateStream)), "Lurker")
    val threshold = actorSystem.actorOf(Props(new Threshold(ProcesssImpl)), "Threshold")
    val watcher = actorSystem.actorOf(Props(new Watcher(threshold, keepers, lurker, stateStream)), "Watcher")
    actorSystem.actorOf(Props[BabbleServer], "BabbleServer")
    val summoner = actorSystem.actorOf(Props(new Summoner(lurker, watcher, keepers)), "Summoner")

    implicit val timeout = Timeout(60 seconds)
    val future = ask(summoner, 'Check)
    Await.result(future, timeout.duration)
    lurker ! 'Flush

    import util.Context.defaultOperations
    actorSystem.scheduler.schedule(1 minute, 5 minutes, watcher, 'Wake)
    actorSystem.scheduler.schedule(2 minutes, 2 minutes, watcher, 'Close)
    actorSystem.scheduler.schedule(10 minutes, 10 minutes, watcher, 'Unlockable)
    actorSystem.scheduler.schedule(3 minutes, 5 minutes, summoner, 'Wake)
  }

  def dispose {
    actorSystem.shutdown()
  }
}
