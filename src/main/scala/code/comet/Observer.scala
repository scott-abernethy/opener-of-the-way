package code.comet

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import net.liftweb.http.{CometListener, CometActor}
import net.liftweb.util.ClearClearable
import xml.NodeSeq
import code.util.DatePresentation
import code.model._
import code.gate.T

class Observer extends CometActor with CometListener {
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactUpdated(a) =>
      //partialUpdate(renderUpdate(a))
      reRender
    case ArtifactCloned(a) =>
      //partialUpdate(renderUpdate(a))
      reRender
    case _ =>
  }
  def render = {
    ClearClearable &
    ".incomplete-item" #> bindIncomplete _ &
    ".complete-item" #> bindComplete _
  }
  def bindIncomplete(in: NodeSeq): NodeSeq = {
    incomplete.flatMap(i => bindIncompleteItem(i._1, i._2, i._3)(in))
  }
  def bindIncompleteItem(clone: Clone, forCultist: Option[Cultist], fromCultist: Cultist)(in: NodeSeq): NodeSeq = {
    val name = for (c <- forCultist) yield c.sign
    ClearClearable &
    ".incomplete-item [id]" #> idFor(clone.id) &
    ".item-what *" #> clone.id &
    ".item-for *" #> name.getOrElse("?") &
    ".item-from *" #> fromCultist.sign &
    ".item-requested *" #> DatePresentation.ago(clone.requested.getTime) &
    ".item-attempted *" #> (if (clone.attempts > 0) DatePresentation.ago(clone.attempted.getTime) else "") &
    ".item-state *" #> clone.state.toString &
    ".item-attempts *" #> formatAttempts(clone.attempts) apply(in)
  }
  def bindComplete(in: NodeSeq): NodeSeq = {
    complete.flatMap(i => bindCompleteItem(i._1, i._2, i._3)(in))
  }
  def bindCompleteItem(clone: Clone, forCultist: Option[Cultist], fromCultist: Cultist)(in: NodeSeq): NodeSeq = {
    val name = for (c <- forCultist) yield c.sign
    ClearClearable &
    ".complete-item [id]" #> idFor(clone.id) &
    ".item-what *" #> clone.id &
    ".item-for *" #> name.getOrElse("?") &
    ".item-from *" #> fromCultist.sign &
    ".item-completed *" #> DatePresentation.ago(clone.attempted.getTime) &
    ".item-elapsed *" #> DatePresentation.duration(clone.attempted.getTime - clone.requested.getTime) &
    ".item-duration *" #> formatDurationSeconds(clone.duration) &
    ".item-attempts *" #> formatAttempts(clone.attempts) apply(in)
  }
  def idFor(id: Long): String = "i" + id
  def incomplete = inTransaction(
    join(clones, cultists.leftOuter, artifacts, gateways, cultists)((c, f, a, g, p) =>
      where(c.state === CloneState.awaiting or c.state === CloneState.cloning)
      select((c, f, p))
      orderBy(c.id desc)
      on(c.forCultistId === f.map(_.id), c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === p.id)
    ).toList
  )
  def complete = inTransaction(
    join(clones, cultists.leftOuter, artifacts, gateways, cultists)((c, f, a, g, p) =>
      where(c.state === CloneState.cloned and c.attempted > T.ago(7 * 24 * 60 * 60 * 1000))
      select((c, f, p))
      orderBy(c.attempted desc)
      on(c.forCultistId === f.map(_.id), c.artifactId === a.id, a.gatewayId === g.id, g.cultistId === p.id)
    ).toList
  )
  def formatAttempts(attempts: Long): String = {
    if (attempts == 0) "" else attempts.toString + "x"
  }
  def formatDurationSeconds(duration: Long): String = {
    if (duration > 0) (duration / 1000) + " sec" else "?"
  }
}
