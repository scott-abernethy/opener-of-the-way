package code.gate

import akka.actor.{ActorRef, Actor}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import net.liftweb.common.Loggable
import code.model._
import code.comet.GatewayServer

case class CloneFailed(c: Clone)

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

      toOpen.foreach( threshold ! OpenGateway(_) )
      toClose.foreach( threshold ! CloseGateway(_) )
    }

    case OpenGateSuccess(g, lp) =>
      markOpen(g, lp)
      lurker ! WayFound(g, lp)

    case OpenGateFailed(g) =>
      markTransient(g)

    case CloseGateSuccess(g) =>
      markClosed(g)

    case CloseGateFailed(g) =>
      markTransient(g)

    case CloneFailed(c) =>
      val (source, sink) = transaction {
        val source = for {
          artifact <- c.artifact
          gateway <- artifact.gateway.headOption
        } yield gateway
        val sink = for {
          requester <- c.forCultist
          gateway <- requester.destination
        } yield gateway
        (source, sink)
      }
      source.foreach( markTransient(_) )
      sink.foreach( markTransient(_) )
      
    case 'Ping =>
      self.reply( 'Pong )
      
    case _ =>
  }

  def markOpen(g: Gateway, lp: String) {
    logger.debug("WayFound " + g)
    transaction {
      updateGate(g, x => {
        x.seen = T.now
        x.state = GateState.open
        x.localPath = lp
        x
      })
    }
    GatewayServer ! 'WayFound
  }

  def markTransient(transient: Gateway) {
    logger.debug("WayTransient " + transient)
    transaction {
      gateways.update(g =>
        where(g.id === transient.id and g.state === GateState.open)
        set(g.state := GateState.transient)
      )
    }
    GatewayServer ! 'WayTransient
  }

  def markClosed(g: Gateway) {
    logger.debug("WayLost " + g)
    transaction {
      updateGate(g, x => {
        x.state = if (g.seen.before(T.ago(4*24*60*60*1000))) GateState.lost else GateState.closed
        x
      })
    }
    GatewayServer ! 'WayLost
  }

  private def updateGate(g: Gateway, updater: (Gateway) => Gateway) {
    gateways.lookup(g.id) match {
      case Some(x) => gateways.update(updater(x))
      case _ => // oop
    }
  }
}