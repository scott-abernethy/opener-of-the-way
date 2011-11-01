package code.comet

import net.liftweb._
import net.liftweb.http._
import net.liftweb.util._
import code.model._
import scala.xml._

class Awaitings extends CometActor with CometListener with ArtifactBinding {
  val factory = new AwaitingSnapshotFactory
  var snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactTouched(_, a) =>
      // todo partialUpdate for all of this...
      snapshot = factory.create(Cultist.attending.is.map(_.id).getOrElse(-1))
      reRender
    case _ =>
  }

  def render = {
    if (snapshot.awaiting.isEmpty) {
      ClearClearable &
      ".awaiting:item" #> NodeSeq.Empty
    } else {
      ClearClearable &
      ".awaiting-empty" #> NodeSeq.Empty &
      ".awaiting:item" #> bindItems(snapshot.awaiting) _
    }
  }

  def bindItems(xs: List[(Artifact, Option[ArtifactState.Value], Clone)])(in: NodeSeq): NodeSeq = {
    xs.flatMap( i =>
      awaitingMessage(i._3).apply( bindItem(in, i._1, i._2, None) )
    )
  }

  def awaitingMessage(clone: Clone): CssSel = {
    if (clone.attempts > 2) {
      ".awaiting:item [class+]" #> "warning-shade" &
      ".item-message *" #> <b>{"Attempted " + clone.attempts + "x without success. Check sink disk usage."}</b>
    } else {
      ".item-message" #> NodeSeq.Empty
    }
  }
}
