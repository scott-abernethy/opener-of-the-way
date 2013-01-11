package gate

import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import model._
import akka.actor.{Props, Scheduler, ActorRef, Actor}
import comet._
import scala.concurrent.duration._
import play.api.Logger
import gate.KeeperRouterApi._
import gate.KeeperApi._

// TODO?
import scala.concurrent.ExecutionContext.Implicits.global

case class CloneFailed(c: Clone)
case class PresenceFailed(p: Presence)
case class Flush(gatewayId: Long)
case class ScourAsap(gatewayId: Long, cultistId: Long)
case class Lock(cultistId: Long)
case class Unlock(cultistId: Long)

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
      g.sink === true and
      (p.map(_.state) === Some(PresenceState.present) or p.map(_.state) === Some(PresenceState.presenting))
    )
    select((cl,g,p))
    orderBy(cl.attempts asc, cl.id asc)
    on(cl.forCultistId === cu.id, cu.id === g.cultistId, cl.artifactId === p.map(_.artifactId))
  )

  def scourQuery(): Query[Gateway] = gateways.where(g =>
    g.source === true and
    (g.scoured < T.ago(Gateway.scourPeriod) or g.scourAsap === true)
  )

  def unopenableQuery(): Query[Gateway] = join(gateways, cultists)( (g, cu) =>
    where(
      (g.state === GateState.transient) or
      (g.state === GateState.open and g.seen > T.ago(Gateway.reopenTestAfter)) or
      (g.requested > g.seen and g.requested > T.ago(Gateway.rerequestableAfter)) or
      (g.failed > T.ago(Gateway.retryFailedAfter)) or
      (cu.shut.isNotNull)
    )
    select(g)
    on(g.cultistId === cu.id)
  )

  val openQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.open
  )

  val transientQuery: Query[Gateway] = gateways.where(g =>
    g.state === GateState.transient
  )

  val gatewaysPresenting: Query[Gateway] = join(presences, artifacts, gateways)( (p, a, g) =>
    where(p.state === PresenceState.presenting and
      g.source === true)
    select(g)
    on(p.artifactId === a.id, a.gatewayId === g.id)
  )

  val gatewaysCloning: Query[Gateway] = join(clones, gateways.leftOuter)( (c, g) =>
    where(c.state === CloneState.cloning and
      g.map(_.sink) === Some(true))
    select(g.get)
    on(c.forCultistId === g.map(_.cultistId))
  )
}

class Watcher(threshold: ActorRef, keepers: ActorRef, lurker: ActorRef, gatewayServer: ActorRef) extends Actor {

  import Watcher._

  var thresholds = Map.empty[String, ActorRef]

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
      transaction {
        val sources = sourcesQuery().toList.map(_._2).distinct
        val sinks = sinksQuery().toList.map(_._2).distinct
        val scour = scourQuery().toList.distinct
        val unopenable = unopenableQuery().toList.toSet
        val transientLocations = transientQuery.toList.map(_.location).toSet
        val open = openQuery.toList.toSet

        Logger.debug("Watcher WANT: " + (Map.empty ++ sources.map("source" -> _) ++ sinks.map("sink" -> _) ++ scour.map("scour" -> _)))

        val wantOpen = (sources ::: sinks ::: scour).distinct.toSet
        val toOpen = (wantOpen -- unopenable).filterNot(transientLocations contains _.location)
        val toTransient = open -- wantOpen
        val keepOpen = open intersect wantOpen

        Logger.debug("Watcher CHANGE: " + (Map.empty ++ toOpen.map("open" -> _) ++ toTransient.map("transient" -> _)))

        val now = T.now
        for (t <- toTransient) {
          Mythos.gateways.update(g =>
            where(g.id === t.id)
            set(g.state := GateState.transient)
          )
          // TODO should send updates outside of transaction
          gatewayServer ! ToState(GateState.transient, t.id, t.cultistId)
          keepers ! ToKeeper(t.id, Closed)
        }

        for (o <- toOpen) {
          Mythos.gateways.update(g =>
              where(g.id === o.id)
          set(g.requested := now)
          )
        }

        toOpen.foreach( g => thresholdFor(g) ! OpenGateway(g) )
        keepOpen.foreach( g => keepers ! ToKeeper(g.id, StillOpen) )
      }
    }
    case 'Close => {
      val watcher = self
      transaction {
        val openLocations = openQuery.toList.map(_.location).toSet
        transientQuery.toList.filterNot(openLocations contains _.location).foreach { g =>
          context.system.scheduler.scheduleOnce(30 seconds, watcher, Flush(g.id))
        }
      }
    }
    case Flush(gatewayId) => {
      transaction {
        gateways.lookup(gatewayId) match {
          case Some(gateway) if (gateway.state == GateState.transient) => {
            val inUseLocations = (gatewaysPresenting.toSet ++ gatewaysCloning.toSet).map(_.location)
            if (!inUseLocations.contains(gateway.location)) {
              thresholdFor(gateway) ! CloseGateway(gateway)
            }
          }
          case _ => // Do nothing
        }
      }
    }
    case ScourAsap(gatewayId, cultistId) => {
      transaction {
        update(gateways)(g =>
          where(g.id === gatewayId)
          set(g.scourAsap := true)
        )
      }
      gatewayServer ! ChangedGateway(gatewayId, cultistId)
      self ! 'Wake
    }
    case Lock(cultistId) => {
      Logger.debug("Lock " + cultistId);
      transaction {
        update(cultists)(c =>
          where(c.id === cultistId)
          set(c.shut := Some(T.future(Cultist.unlockAfter)))
        )
      }
      // TODO force cloner and presenter to stop using cultists gateways now via keeper
      gatewayServer ! ChangedGateways(cultistId)
    }
    case Unlock(cultistId) => {
      Logger.debug("Unlock " + cultistId);
      transaction {
        update(cultists)(c =>
          where(c.id === cultistId and c.shut.isNotNull)
          set(c.shut := None)
        )
      }
      gatewayServer ! ChangedGateways(cultistId)
      self ! 'Wake
    }
    case 'Unlockable => {
      transaction {
        update(cultists)(c =>
          where(c.shut.isNotNull and c.shut < Some(T.now))
          set(c.shut := None)
        )
      }
    }
    case OpenGateSuccess(g, lp) => {
      markOpen(g, lp)
      lurker ! WayFound(g, lp)
    }
    case OpenGateFailed(g) => {
      markTransient(g)
    }
    case GateFailed(gid) => {
      Gateway.find(gid) foreach markTransient
    }
    case CloseGateSuccess(g) => {
      markClosed(g)
    }
    case CloseGateFailed(g) => {
      markTransient(g)
    }
    case PresenceFailed(p) => {
      val source = transaction {
        for {
          artifact <- p.artifact
          gateway <- artifact.gateway.headOption
        } yield gateway
      }
      source.foreach( markFailedThusTransient(_) )
    }
    case CloneFailed(c) => {
      val sink = transaction {
        for {
          requester <- c.forCultist
          gateway <- requester.destination
        } yield gateway
      }
      sink.foreach( markFailedThusTransient(_) )
    }
    case 'Ping => {
      sender ! 'Pong
    }
    case msg => {
      unhandled(msg)
    }
  }

  def markOpen(g: Gateway, lp: String) {
    Logger.debug("WayFound " + g)
    transaction {
      updateGate(g, x => {
        val now = T.now
        x.seen = now
        x.state = GateState.open
        x.localPath = lp
        x
      })
    }
    gatewayServer ! ToState(GateState.open, g.id, g.cultistId)
    keepers ! ToKeeper(g.id, Open)
  }

  def markTransient(transient: Gateway) {
    Logger.debug("WayTransient " + transient)
    transaction {
      gateways.update(g =>
        where(g.id === transient.id and g.state === GateState.open)
        set(g.state := GateState.transient)
      )
    }
    gatewayServer ! ToState(GateState.transient, transient.id, transient.cultistId)
    keepers ! ToKeeper(transient.id, Closed)
  }

  def markFailedThusTransient(transient: Gateway) {
    Logger.debug("WayFailed " + transient)
    transaction {
      gateways.update(g =>
        where(g.id === transient.id and g.state === GateState.open)
        set(g.state := GateState.transient, g.failed := T.now)
      )
    }
    gatewayServer ! ToState(GateState.transient, transient.id, transient.cultistId)
    keepers ! ToKeeper(transient.id, Closed)
  }

  def markClosed(g: Gateway) {
    // TODO is this the only way a gate can go lost? wrong if so.
    Logger.debug("WayLost " + g)
    transaction {
      updateGate(g, x => {
        x.state = if (g.seen.before(T.ago(4*24*60*60*1000))) GateState.lost else GateState.closed
        x
      })
    }
    gatewayServer ! ToState(GateState.closed, g.id, g.cultistId)
    keepers ! ToKeeper(g.id, Closed)
  }

  private def updateGate(g: Gateway, updater: (Gateway) => Gateway) {
    gateways.lookup(g.id) match {
      case Some(x) => gateways.update(updater(x))
      case _ => // oop
    }
  }

  def thresholdFor(gateway: Gateway): ActorRef = {
    thresholds.get(gateway.location) getOrElse {
      val t = context.actorOf(Props(new Threshold(ProcesssImpl)))
      thresholds += (gateway.location -> t)
      t
    }
  }
}
