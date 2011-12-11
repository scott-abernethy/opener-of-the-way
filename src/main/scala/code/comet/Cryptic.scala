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
    ".cloned-item" #> cloned().map{ i =>
      val (clone, artifact, presence, forCultist, profferredBy) = i
      val state = artifact.stateFor(
        forCultist.id,
        profferredBy.id,
        Some(clone),
        t,
        presence
      )
      val symbol: Node = state match {
        case Some(ArtifactState.cloned) => {
          Unparsed("&gt;")
        }
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
      val emphasis: Option[String] = for {
        s <- state
        if (s != ArtifactState.cloned)
      }
      yield "emphasis"
      "*" #> writeSymbol(symbol, List(artifactDesc(artifact, profferredBy), clonerDesc(forCultist)).mkString(", "), cloneWaitClass(clone).toList ::: emphasis.toList)
    } &
    ".glimpsed-item" #> glimpsed().map{ g =>
      val (artifact, presence, profferredBy) = g
      val symbol: Node = presence.map(_.state) match {
        case Some(PresenceState.present) => {
          Unparsed("&le;");
        }
        case _ => {
          Unparsed("&lt;")
        }
      }
      "*" #> writeSymbol(symbol, artifactDesc(artifact, profferredBy))
    }
  }

  def artifactDesc(artifact: Artifact, profferedBy: Cultist): String = {
    artifact.path + " from " + profferedBy.sign
  }

  def clonerDesc(cloner: Cultist): String = {
    "for " + cloner.sign
  }

  def cloneWaitClass(clone: Clone): Option[String] = {
    val wait = clone.waitPlusDuration()
    if (wait > Clone.terribleWaitAfter) {
      Some("error")
    }
    else if (wait > Clone.poorWaitAfter) {
      Some("warning")
    }
    else if (wait > Clone.marginalWaitAfter) {
      Some("notice")
    }
    else {
      None
    }
  }

  def writeSymbol(inner: NodeSeq, description: String, otherClasses: List[String] = Nil): NodeSeq = {
    <span class={ ("symbol" :: otherClasses).mkString(" ") }><abbr title={ description }>{ inner }</abbr></span>
  }

  def glimpsed() = inTransaction(
    join(artifacts, presences.leftOuter, gateways, cultists)((a, p, g, o) =>
      where(a.discovered > T.startOfDay(T.now))
      select((a, p, o))
      orderBy(a.discovered asc, a.id asc)
      on(a.id === p.map(_.artifactId), a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def cloned() = inTransaction(
    join(clones, presences.leftOuter, cultists, artifacts, gateways, cultists)((c, p, f, a, g, o) =>
      where(
        (c.state === CloneState.cloned and c.attempted > T.startOfDay(T.now)) or
        (c.state <> CloneState.cloned)
      )
      select((c, a, p, f, o))
      orderBy(c.requested asc, c.id asc)
      on(c.artifactId === p.map(_.artifactId), c.forCultistId === f.id, c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def formatAttempts(attempts: Long): String = {
    if (attempts == 0) "" else attempts.toString + "x"
  }

  def formatDurationSeconds(duration: Long): String = {
    if (duration > 0) (duration / 1000) + " sec" else "?"
  }
}
