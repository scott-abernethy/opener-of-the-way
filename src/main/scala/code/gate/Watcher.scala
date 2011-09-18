package code.gate

import akka.actor.{ActorRef, Actor}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import net.liftweb.common.Loggable
import code.model._

object Watcher {

  // TODO should also include check for reattempt timeout, as Manipulator does...
  def readyClonesQuery(): Query[Clone] = join(clones, artifacts)( (c, a) =>
    where(c.state <> CloneState.cloned and
      a.witnessed > T.ago(Artifact.lostAfter) and
      a.discovered < T.ago(Artifact.immatureBefore))
    select(c)
    on(c.artifactId === a.id)
  )

  def sourcesQuery(): Query[Gateway] = join(clones, artifacts, gateways)( (c, a, g) =>
    where(c.id in from(readyClonesQuery())(c => select(c.id)))
    select(g)
    on(c.artifactId === a.id, a.gatewayId === g.id)
  )

  def sinksQuery(): Query[Gateway] = join(clones, cultists, gateways)( (cl, cu, g) =>
    where(cl.id in from(readyClonesQuery())(c => select(c.id)) and g.mode === GateMode.sink)
    select(g)
    on(cl.forCultistId === cu.id, cu.id === g.cultistId)
  )

  val scourQuery: Query[Gateway] = gateways.where(g =>
    g.mode === GateMode.source and
    g.scoured < T.ago(3 * 60 * 60 * 1000)
  )

  val openQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.open
  )
}

class Watcher(threshold: ActorRef, lurker: scala.actors.Actor) extends Actor with Loggable {

  import Watcher._

  def receive = {
    case 'Wake => {
      val (sources, sinks, scour, open) = transaction {
        (sourcesQuery().toList.distinct, sinksQuery.toList.distinct, scourQuery.toList.distinct, openQuery.toList.distinct)
      }

      logger.info("Watcher open " + open)
      logger.info("Watcher want source " + sources + " sinks " + sinks + " scour " + scour)
      
      val toOpen = (sources ::: sinks ::: scour).distinct

      for (g <- toOpen/* if !open.contains(g)*/) {
        threshold ! OpenGateway(g)
      }
      for (g <- open if !toOpen.contains(g)) {
        threshold ! CloseGateway(g)
      }
    }
    case m @ WayFound(_, _) => lurker ! m
    case m @ WayLost(_) => lurker ! m
    case _ =>
  }
}