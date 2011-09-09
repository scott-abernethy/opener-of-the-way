package code.gate

import akka.actor.{ActorRef, Actor}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import code.model.{GateMode, Gateway, CloneState, GateState}

class Watcher(threshold: ActorRef) extends Actor {

  val sourcesQuery: Query[Gateway] = join(clones, artifacts, gateways)( (c, a, g) =>
    where(c.state <> CloneState.cloned)
    select(g)
    on(c.artifactId === a.id, a.gatewayId === g.id)
  )

  val sinksQuery: Query[Gateway] = join(clones, cultists, gateways)( (cl, cu, g) =>
    where(cl.state <> CloneState.cloned and g.mode === GateMode.sink)
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

  def receive = {
    case 'Wake => {
      val (sources, sinks, scour, open) = transaction {
        (sourcesQuery.toList, sinksQuery.toList, scourQuery.toList, openQuery.toList)
      }

      val toOpen = (sources ::: sinks ::: scour).distinct

      for (g <- toOpen if !open.contains(g)) {
        threshold ! OpenGateway(g)
      }
      for (g <- open if !toOpen.contains(g)) {
        threshold ! CloseGateway(g)
      }
    }
  }
}