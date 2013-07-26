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

package model

import gate._
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

  implicit val timeout = Timeout(60 seconds)
    
  def start {
    val config: Config = Play.configuration(Play.current).underlying
    
    actorSystem = ActorSystem.create("YogSothoth", config.getConfig("ootw").withFallback(config))
    
    val devourer = actorSystem.actorOf(Props(new Devourer()), "Devourer")
    val historyEaten = ask(devourer, 'Wake)
    Await.result(historyEaten, timeout.duration)
    
    val artifactServer = actorSystem.actorOf(Props[ArtifactServer], "ArtifactServer")
    val keepers = actorSystem.actorOf(Props(new KeeperRouter(artifactServer)), "Keepers")
    val stateStream = actorSystem.actorOf(Props[StateStream], "StateStream")
    val lurker = actorSystem.actorOf(Props(new Lurker(actorSystem.deadLetters, artifactServer, stateStream)), "Lurker")
    val threshold = actorSystem.actorOf(Props(new Threshold(ProcesssFactory.create)), "Threshold")
    val watcher = actorSystem.actorOf(Props(new Watcher(threshold, keepers, lurker, stateStream)), "Watcher")
    actorSystem.actorOf(Props[BabbleServer], "BabbleServer")
    val summoner = actorSystem.actorOf(Props(new Summoner(lurker, watcher, keepers)), "Summoner")

    val transferStatesCleaned = ask(summoner, 'Check)
    Await.result(transferStatesCleaned, timeout.duration)
    lurker ! 'Flush

    import util.Context.defaultOperations
    actorSystem.scheduler.schedule(1 minute, 5 minutes, watcher, 'Wake)
    actorSystem.scheduler.schedule(2 minutes, 2 minutes, watcher, 'Close)
    actorSystem.scheduler.schedule(10 minutes, 10 minutes, watcher, 'Unlockable)
    actorSystem.scheduler.schedule(3 minutes, 5 minutes, summoner, 'Wake)
    actorSystem.scheduler.schedule(1 day, 1 day, devourer, 'Wake)
  }

  def dispose {
    actorSystem.shutdown()
  }
}
