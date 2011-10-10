package code.gate

import akka.actor.{ActorRef, Actor}
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import net.liftweb.common.Loggable
import code.model._
import code.comet.GatewayServer

case class CloneFailed(c: Clone)
case class PresenceFailed(p: Presence)

object Watcher {

  def sourcesQuery(): Query[(Presence,Gateway)] = join(presences, artifacts, gateways)( (p, a, g) =>
    where(
      (p.state === PresenceState.called or p.state === PresenceState.presenting) and
      (p.attempts === 0 or p.attempted < T.ago(Clone.nonRepeatableBefore)) and
      a.witnessed > T.ago(Artifact.lostAfter) and
      a.discovered < T.ago(Artifact.immatureBefore)
    )
    select((p,g))
    orderBy(p.attempts asc, p.id asc)
    on(p.artifactId === a.id, a.gatewayId === g.id)
  )
  
  def sinksQuery(): Query[(Clone,Gateway,Option[Presence])] = join(clones, cultists, gateways, presences.leftOuter)( (cl, cu, g, p) =>
    where(
      cl.state <> CloneState.cloned and
      (cl.attempts === 0 or cl.attempted < T.ago(Clone.nonRepeatableBefore)) and
      g.mode === GateMode.sink and
      p.map(_.state) === Some(PresenceState.present)
    )
    select((cl,g,p))
    orderBy(cl.attempts asc, cl.id asc)
    on(cl.forCultistId === cu.id, cu.id === g.cultistId, cl.artifactId === p.map(_.artifactId))
  )

  val scourQuery: Query[Gateway] = gateways.where(g =>
    g.mode === GateMode.source and
    g.scoured < T.ago(Gateway.scourPeriod)
  )

  val openQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.open
  )

  val transientQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.transient
  )

  val presentingQuery: Query[Presence] = presences.where(p =>
    p.state === PresenceState.presenting
  )

  val cloningQuery: Query[Clone] = clones.where(c =>
    c.state === CloneState.cloning
  )
}

class Watcher(threshold: ActorRef, lurker: scala.actors.Actor) extends Actor with Loggable {

  import Watcher._

  def receive = {
    case 'Wake => {
      // TODO waking should be a backup mechanism for doing this. Do on demand.
      val (toOpen, toClose) = transaction {
        val sources = sourcesQuery().toList.map(_._2).distinct
        val sinks = sinksQuery.toList.map(_._2).distinct
        val scour = scourQuery.toList.distinct
        val open = openQuery.toList.distinct
        val transient = transientQuery.toSet
        val isPresenting = presentingQuery.toList.size > 0
        val isCloning = cloningQuery.toList.size > 0

        logger.info("Watcher OPEN: " + open)
        logger.info("Watcher WANT source: " + sources)
        logger.info("Watcher WANT sink " + sinks)
        logger.info("Watcher WANT scour: " + scour)
        logger.info("Watcher CLOSE transient: " + transient)

        val toOpen = (sources ::: sinks ::: scour).distinct.toSet -- transient // don't open transient.
        // TODO optimize closing while busy
        var toClose = if (isPresenting || isCloning) Set.empty[Gateway] else transient
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

    case PresenceFailed(p) =>
      val source = transaction {
        for {
          artifact <- p.artifact
          gateway <- artifact.gateway.headOption
        } yield gateway
      }
      source.foreach( markTransient(_) )

    case CloneFailed(c) =>
      val sink = transaction {
        for {
          requester <- c.forCultist
          gateway <- requester.destination
        } yield gateway
      }
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