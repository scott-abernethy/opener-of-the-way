package code.comet

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import net.liftweb.http.{CometListener, CometActor}
import code.util.DatePresentation
import code.model._
import xml.{Unparsed, Node, Text, NodeSeq}
import code.gate.{Millis, T}
import java.sql.Timestamp
import net.liftweb.util.{CssSel, ClearClearable}

class Cryptic extends CometActor with CometListener {
  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactTouched(_, _) => {
      reRender(true)
    }
    case _ => { }
  }



  def render = {
    val t = T.startOfDay(T.now)
    val history = for {
      offset <- List.range(0, 7)
    }
    yield {
      val point = T.agoFrom(t, Millis.days(offset))
      val start = T.startOfDay(point)
      val end = T.futureFrom(start, Millis.days(1))
      val clones = if (offset == 0) {
        loadClonesAndAwaiting(start, end)
      }
      else {
        loadClones(start, end)
      }
      val glimpsed = loadGlimpsed(start, end)
      ( historyFor(clones, t), impressionFor(glimpsed) )
    }

    ClearClearable &
    ".cryptic-history" #> history.map { day =>
      val (clones, glimpsed) = day
      ".cloned-count *" #> sectionKey(clones.size, ">") &
      ".cloned-item" #> sectionBody(clones) &
      ".glimpsed-count *" #> sectionKey(glimpsed.size, "<") &
      ".glimpsed-item" #> sectionBody(glimpsed)
    }
  }

  def sectionKey(itemCount: Int, icon: String): NodeSeq = {
//    if (itemCount > 0) {
      Text(itemCount + " " + icon)
//    }
//    else {
//      Text("-")
//    }
  }

  def sectionBody(items: List[CssSel]): List[CssSel] = {
    if (items.size > 0) {
      items
    }
    else {
      List(".replace" #> Unparsed("&nbsp;"))
    }
  }

  def historyFor(items: List[(Clone, Artifact, Option[Presence], Cultist, Cultist)], at: Timestamp): List[CssSel] = {
    items.map { i =>
      val (clone, artifact, presence, forCultist, profferredBy) = i
      val state = artifact.stateFor(
        forCultist.id,
        profferredBy.id,
        Some(clone),
        at,
        presence
      )
      val symbol: Node = state match {
        case Some(ArtifactState.cloned) => {
          Unparsed("&gt;")
        }
        case Some(ArtifactState.cloning) => {
          //Unparsed("&asymp;")
          Unparsed("&#8734;")
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
      ".replace" #> writeSymbol(symbol, List(artifactDesc(artifact, profferredBy), clonerDesc(forCultist)).mkString(", "), cloneWaitClass(clone).toList ::: emphasis.toList)
    }
  }

  def impressionFor(items: List[(Artifact, Option[Presence], Cultist)]): List[CssSel] = {
    items.map { i =>
      val (artifact, presence, profferredBy) = i
      val symbol: Node = presence.map(_.state) match {
        case Some(PresenceState.present) => {
          Unparsed("&le;");
        }
        case _ => {
          Unparsed("&lt;")
        }
      }
      ".replace" #> writeSymbol(symbol, artifactDesc(artifact, profferredBy))
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
    <a href="#" class={ ("symbol" :: otherClasses).mkString(" ") } title={ description }>{ inner }</a>
  }

  def loadGlimpsed(startInclusive: Timestamp, endExclusive: Timestamp) = inTransaction(
    join(artifacts, presences.leftOuter, gateways, cultists)((a, p, g, o) =>
      where(a.discovered >= startInclusive and
        a.discovered < endExclusive)
      select((a, p, o))
      orderBy(a.discovered asc, a.id asc)
      on(a.id === p.map(_.artifactId), a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def loadClones(startInclusive: Timestamp, endExclusive: Timestamp) = inTransaction(
    join(clones, presences.leftOuter, cultists, artifacts, gateways, cultists)((c, p, f, a, g, o) =>
      where(c.state === CloneState.cloned and
        c.attempted >=  startInclusive and
        c.attempted < endExclusive
      )
      select((c, a, p, f, o))
      orderBy(c.requested asc, c.id asc)
      on(c.artifactId === p.map(_.artifactId), c.forCultistId === f.id, c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === o.id)
    ).toList
  )

  def loadClonesAndAwaiting(startInclusive: Timestamp, endExclusive: Timestamp) = inTransaction(
    join(clones, presences.leftOuter, cultists, artifacts, gateways, cultists)((c, p, f, a, g, o) =>
      where(
        (c.state === CloneState.cloned and
          c.attempted >=  startInclusive and
          c.attempted < endExclusive
        ) or
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
