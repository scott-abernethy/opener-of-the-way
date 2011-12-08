package code.comet

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import net.liftweb.http.{CometListener, CometActor}
import net.liftweb.util.ClearClearable
import code.util.DatePresentation
import code.model._
import code.gate.T
import xml.{Unparsed, Node, Text, NodeSeq}

class Cryptic extends CometActor with CometListener {
  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactTouched(_, _) => {
      reRender(true)
    }
    case _ => { }
  }

  def render = {
    val t = T.now
    ClearClearable &
    ".queue-item" #> incomplete().map{ i =>
      val (clone, artifact, presence, forCultist, profferredBy) = i
      val state = artifact.stateFor(
        forCultist.map(_.id).getOrElse(-1),
        profferredBy.id,
        Some(clone),
        t,
        presence
      )
      val symbol: Node = state match {
        case Some(ArtifactState.cloning) => {
          Unparsed("&asymp;")
        }
        case Some(ArtifactState.awaitingPresent) => {
          Text("=")
        }
        case Some(ArtifactState.awaitingLost) => {
          Unparsed("&ne;")
        }
        case Some(ArtifactState.awaiting) => {
          Unparsed("&minus;")
        }
        case _ => {
          Unparsed("&nbsp;")
        }
      }
      "*" #> writeSymbol(symbol, List(artifactDesc(artifact, profferredBy), clonerDesc(forCultist)).mkString(", "))
    } &
    ".glimpsed-item" #> glimpsed().map{ g =>
      val (artifact, presence, profferredBy) = g
      println(presence)
      val symbol: Node = presence.map(_.state) match {
        case Some(PresenceState.present) => {
          Unparsed("&le;");
        }
        case _ => {
          Unparsed("&lt;")
        }
      }
      "*" #> writeSymbol(symbol, artifactDesc(artifact, profferredBy))
    } &
    ".cloned-item" #> complete().map{ c =>
      val (clone, artifact, presence, forCultist, profferredBy) = c
      "*" #> writeSymbol(Unparsed("&gt;"), List(artifactDesc(artifact, profferredBy), clonerDesc(forCultist)).mkString(", "))
    }
  }

  def artifactDesc(artifact: Artifact, profferedBy: Cultist): String = {
    artifact.path + " from " + profferedBy.sign
  }

  def clonerDesc(cloner: Option[Cultist]): String = {
    "for " + cloner.map(_.sign).getOrElse("Unknown")
  }

  def writeSymbol(inner: NodeSeq, description: String): NodeSeq = {
    <span class="symbol"><abbr title={ description }>{ inner }</abbr></span>
  }

  def incomplete() = inTransaction(
    join(clones, presences.leftOuter, cultists.leftOuter, artifacts, gateways, cultists)((c, p, f, a, g, o) =>
      where(c.state === CloneState.awaiting or c.state === CloneState.cloning)
      select((c, a, p, f, o))
      orderBy(c.requested desc)
      on(c.artifactId === p.map(_.artifactId), c.forCultistId === f.map(_.id), c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def glimpsed() = inTransaction(
    join(artifacts, presences.leftOuter, gateways, cultists)((a, p, g, o) =>
      where(a.discovered > T.startOfDay(T.now))
      select((a, p, o))
      orderBy(a.discovered desc)
      on(a.id === p.map(_.artifactId), a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def complete() = inTransaction(
    join(clones, presences.leftOuter, cultists.leftOuter, artifacts, gateways, cultists)((c, p, f, a, g, o) =>
      where(c.state === CloneState.cloned and c.attempted > T.startOfDay(T.now))
      select((c, a, p, f, o))
      orderBy(c.attempted desc)
      on(c.artifactId === p.map(_.artifactId), c.forCultistId === f.map(_.id), c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def formatAttempts(attempts: Long): String = {
    if (attempts == 0) "" else attempts.toString + "x"
  }

  def formatDurationSeconds(duration: Long): String = {
    if (duration > 0) (duration / 1000) + " sec" else "?"
  }
}
