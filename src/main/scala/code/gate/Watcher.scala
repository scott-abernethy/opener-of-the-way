package code.gate

import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import net.liftweb.common.Loggable
import code.model._
import code.comet.GatewayServer
import akka.actor.{Scheduler, ActorRef, Actor}

case class CloneFailed(c: Clone)
case class PresenceFailed(p: Presence)
case class FlushAndClose(gatewayId: Long)

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

  def scourQuery(): Query[Gateway] = gateways.where(g =>
    g.mode === GateMode.source and
    g.scoured < T.ago(Gateway.scourPeriod)
  )

  val openQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.open
  )

  val transientQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.transient
  )

  val gatewaysPresenting: Query[Gateway] = join(presences, artifacts, gateways)( (p, a, g) =>
    where(p.state === PresenceState.presenting and
      g.mode === GateMode.source)
    select(g)
    on(p.artifactId === a.id, a.gatewayId === g.id)
  )

  val gatewaysCloning: Query[Gateway] = join(clones, gateways.leftOuter)( (c, g) =>
    where(c.state === CloneState.cloning and
      g.map(_.mode) === Some(GateMode.sink))
    select(g.get)
    on(c.forCultistId === g.map(_.cultistId))
  )
}

class Watcher(threshold: ActorRef, lurker: scala.actors.Actor) extends Actor with Loggable {

  import Watcher._

  def receive = {
    case 'Source => {
      // Dummy implementation initally
      self ! 'Wake
    }
    case 'Sink => {
      // Dummy implementation initally
      self ! 'Wake
    }
    case 'Wake => {
      // TODO waking should be a backup mechanism for doing this. Do on demand.
      // TODO split into multiple methods, open / close / transient / source / sink
      transaction {
        val sources = sourcesQuery().toList.map(_._2).distinct
        val sinks = sinksQuery().toList.map(_._2).distinct
        val scour = scourQuery().toList.distinct
        val open = openQuery.toList.distinct
        val reopenTimestamp = T.ago(Gateway.reopenTestAfter)
        val dontReopen = open.filter(g => g.seen.after(reopenTimestamp))

        // TODO much of the system latency is due to the 5 min Wake cycle.
        
        logger.info("Watcher OPEN: " + open)
        logger.info("Watcher WANT source: " + sources)
        logger.info("Watcher WANT sink " + sinks)
        logger.info("Watcher WANT scour: " + scour)

        val keepOpen = (sources ::: sinks ::: scour).distinct.toSet.filter(_.state != GateState.transient) // don't open transient.
        val rerequestTimeStamp = T.ago(Gateway.rerequestableAfter)
        val toOpen = (keepOpen -- dontReopen).filter(g => g.seen.after(g.requested) || g.requested.before(rerequestTimeStamp))
        
        val toTransient = open.toSet -- keepOpen

        val now = T.now
        for (t <- toTransient) {
          Mythos.gateways.update(g =>
            where(g.id === t.id)
            set(g.state := GateState.transient)
          )
        }
        for (o <- toOpen) {
          Mythos.gateways.update(g =>
            where(g.id === o.id)
            set(g.requested := now)
          )
        }

        toOpen.foreach( threshold ! OpenGateway(_) )
      }
    }
    case 'Close => {
      transaction {
        transientQuery.toList match {
          case Nil => // Do nothing
          case transient => {
            val inUse = gatewaysPresenting.toSet ++ gatewaysCloning.toSet
            var toClose = transient.toSet -- inUse
            toClose.foreach( threshold ! CloseGateway(_) )
          }
        }

      }
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
  }

  def markOpen(g: Gateway, lp: String) {
    logger.debug("WayFound " + g)
    transaction {
      updateGate(g, x => {
        val now = T.now
        x.seen = now
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
    // TODO is this the only way a gate can go lost? wrong if so.
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