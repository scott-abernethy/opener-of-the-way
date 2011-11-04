package code.comet

import net.liftweb._
import net.liftweb.http._
import js.jquery.JqJsCmds
import js.JsCmds
import net.liftweb.util._
import code.model._
import scala.xml._
import code.js.JquiJsCmds
import util.Helpers._

class Awaitings extends CometActor with CometListener with ArtifactBinding {
  val cultistId = Cultist.attending.is.map(_.id).getOrElse(-1L)
  val factory = new AwaitingSnapshotFactory
  var snapshot = factory.create(cultistId)
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactTouched(_, a) =>
      factory.stateOf(cultistId, a).foreach { i =>
        val a = i._1
        val s = i._2
        val c = i._3
        val (snapshot2, action) = snapshot.update(a, s, c)
        snapshot = snapshot2
        val id: String = "a" + a.id
        val update = action match {
          case Add =>
            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _ andThen ".awaiting:item [class+]" #> "hidden").apply(defaultHtml)
            JqJsCmds.AppendHtml("awaitings", out ) & JquiJsCmds.BlindInFast(id)
          case Update =>
            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _).apply(defaultHtml)
            JsCmds.Replace(id, out )
          case _ =>
            JquiJsCmds.BlindOutFast(id) & JsCmds.After(1 second, JsCmds.Replace(id, NodeSeq.Empty))
        }
        partialUpdate(update)
      }
    case _ =>
  }

  def render = {
    if (snapshot.awaiting.isEmpty) {
      ClearClearable &
      "#awaiting-empty [class+]" #> "hidden" &
      ".awaiting:item" #> NodeSeq.Empty
    } else {
      ClearClearable &
      "#awaiting-empty [class+]" #> "hidden" &
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
