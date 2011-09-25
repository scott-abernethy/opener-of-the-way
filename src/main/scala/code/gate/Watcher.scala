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

  val transientQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.transient
  )

  val cloningQuery: Query[Clone] = clones.where(c =>
    c.state === CloneState.cloning
  )
}

class Watcher(threshold: ActorRef, lurker: scala.actors.Actor) extends Actor with Loggable {

  import Watcher._

  def receive = {
    case 'Wake => {
      val (toOpen, toClose) = transaction {
        val sources = sourcesQuery().toList.distinct
        val sinks = sinksQuery.toList.distinct
        val scour = scourQuery.toList.distinct
        val open = openQuery.toList.distinct
        val transient = transientQuery.toSet
        val isCloning = cloningQuery.toList.size > 0

        logger.info("Watcher OPEN: " + open)
        logger.info("Watcher WANT source: " + sources)
        logger.info("Watcher WANT sink " + sinks)
        logger.info("Watcher WANT scout: " + scour)
        logger.info("Watcher CLOSE transient: " + transient)

        val toOpen = (sources ::: sinks ::: scour).distinct.toSet -- transient // don't open transient.
        var toClose = if (isCloning) Set.empty[Gateway] else transient
        val toTransient = (open.toSet -- toOpen) -- transient

        for (t <- toTransient) {
          Mythos.gateways.update(g =>
            where(g.id === t.id)
            set(g.state := GateState.transient)
          )
        }

        (toOpen, toClose)
      }

      for (g <- toOpen) {
        threshold ! OpenGateway(g)
      }
      for (g <- toClose) {
        threshold ! CloseGateway(g)
      }
    }
    case m @ WayFound(_, _) => lurker ! m
    case m @ WayLost(_) => lurker ! m
    case _ =>
  }
}