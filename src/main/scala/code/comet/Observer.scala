package code.comet

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import net.liftweb.http.{CometListener, CometActor}
import net.liftweb.util.ClearClearable
import xml.NodeSeq
import code.model.{Cultist, ArtifactState, Artifact, Clone}
import code.util.DatePresentation

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
    ".incomplete-item" #> bindIncomplete _
  }
  def bindIncomplete(in: NodeSeq): NodeSeq = {
    incomplete.flatMap(i => bindItem(i._1, i._2, i._3)(in))
  }
  def bindItem(clone: Clone, artifact: Option[Artifact], forCultist: Option[Cultist])(in: NodeSeq): NodeSeq = {
    val name = for (c <- forCultist) yield c.sign
    ClearClearable &
    ".incomplete-item [id]" #> idFor(clone.id) &
    ".item-what *" #> clone.id &
    ".item-for *" #> name.getOrElse("?") &
    ".item-requested *" #> DatePresentation.ago(clone.requested.getTime) &
    ".item-state *" #> clone.state.toString &
    ".item-attempts *" #> clone.attempts.toString apply(in)
  }
  def idFor(id: Long): String = "i" + id
  def incomplete = inTransaction(
    join(clones, artifacts.leftOuter, cultists.leftOuter)((c, a, f) =>
      select((c, a, f))
      orderBy(c.id asc)
      on(c.artifactId === a.map(_.id), c.forCultistId === f.map(_.id))
    ).toSeq
  )
}
