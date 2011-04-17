package code.comet

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import net.liftweb.http.{CometListener, CometActor}
import net.liftweb.util.ClearClearable
import xml.NodeSeq
import code.util.DatePresentation
import code.model._

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
    incomplete.flatMap(i => bindIncompleteItem(i._1, i._2)(in))
  }
  def bindIncompleteItem(clone: Clone, forCultist: Option[Cultist])(in: NodeSeq): NodeSeq = {
    val name = for (c <- forCultist) yield c.sign
    ClearClearable &
    ".incomplete-item [id]" #> idFor(clone.id) &
    ".item-what *" #> clone.id &
    ".item-for *" #> name.getOrElse("?") &
    ".item-requested *" #> DatePresentation.ago(clone.requested.getTime) &
    ".item-attempted *" #> (if (clone.attempts > 0) DatePresentation.ago(clone.attempted.getTime) else "") &
    ".item-state *" #> clone.state.toString &
    ".item-attempts *" #> formatAttempts(clone.attempts) apply(in)
  }
  def bindComplete(in: NodeSeq): NodeSeq = {
    complete.flatMap(i => bindCompleteItem(i._1, i._2)(in))
  }
  def bindCompleteItem(clone: Clone, forCultist: Option[Cultist])(in: NodeSeq): NodeSeq = {
    val name = for (c <- forCultist) yield c.sign
    ClearClearable &
    ".complete-item [id]" #> idFor(clone.id) &
    ".item-what *" #> clone.id &
    ".item-for *" #> name.getOrElse("?") &
    ".item-completed *" #> DatePresentation.ago(clone.attempted.getTime) &
    ".item-elapsed *" #> DatePresentation.duration(clone.attempted.getTime - clone.requested.getTime) &
    ".item-duration *" #> formatDurationSeconds(clone.duration) &
    ".item-attempts *" #> formatAttempts(clone.attempts) apply(in)
  }
  def idFor(id: Long): String = "i" + id
  def incomplete = inTransaction(
    join(clones, cultists.leftOuter)((c, f) =>
      where(c.state === CloneState.queued or c.state === CloneState.progressing)
      select((c, f))
      orderBy(c.id desc)
      on(c.forCultistId === f.map(_.id))
    ).toSeq
  )
  def complete = inTransaction(
    join(clones, cultists.leftOuter)((c, f) =>
      where(c.state === CloneState.done)
      select((c, f))
      orderBy(c.attempted desc)
      on(c.forCultistId === f.map(_.id))
    ).page(0, 15).toSeq
  )
  def formatAttempts(attempts: Long): String = {
    if (attempts == 0) "" else attempts.toString + "x"
  }
  def formatDurationSeconds(duration: Long): String = {
    if (duration > 0) (duration / 1000) + " sec" else "?"
  }
}
