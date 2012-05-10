package code.comet

import net.liftweb._
import common.Loggable
import net.liftweb.http._
import js.jquery.JqJsCmds
import js.JsCmds
import net.liftweb.util._
import code.model._
import scala.xml._
import code.js.JquiJsCmds
import util.Helpers._
import code.state.{ArtifactPack, ArtifactServer, ArtifactTouched}

class Awaitings extends CometActor with CometListener with ArtifactBinding with Loggable {
  val cultistId = Cultist.attending.is.map(_.id).getOrElse(-1L)
  val factory = new AwaitingSnapshotFactory
  var snapshot = factory.create(cultistId)

  def registerWith = ArtifactServer

  override def lowPriority = {
    case pack @ ArtifactPack(_, a, _, _, _) => {
      // todo shortcut, check for cultist is matching.
      val s: Option[ArtifactState.Value] = pack.stateFor(cultistId)
      val c: Option[Clone] = pack.cloneFor(cultistId)
      val (snapshot2, action) = snapshot.update(a, s, c)
        snapshot = snapshot2

        val update = action match {
          case Add(cid) => {
            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _ andThen ".awaiting:item [class+]" #> "hidden").apply(defaultHtml)
            partialUpdate( JqJsCmds.AppendHtml("awaitings", out ) & JquiJsCmds.BlindInFast(idOf(cid)) )
          }
          case Update(cid) => {
            val out: NodeSeq = (".awaiting:item ^^" #> "notused" andThen ".awaiting:item" #> bindItems((a, s, c.get) :: Nil) _).apply(defaultHtml)
            partialUpdate( JsCmds.Replace(idOf(cid), out ) )
          }
          case Remove(cid) => {
            partialUpdate( JquiJsCmds.BlindOutFast(idOf(cid)) & JsCmds.After(1 second, JsCmds.Replace(idOf(cid), NodeSeq.Empty)) )
          }
          case Other => {
            reRender()
          }
          case _ =>
        }
      }
    case _ => {}
  }

  def idOf(cloneId: Long) = { "aw" + cloneId }

  def render = {
    ClearClearable &
    ".awaiting:item" #> bindItems(snapshot.awaiting) _
  }

  def bindItems(xs: List[(Artifact, Option[ArtifactState.Value], Clone)])(in: NodeSeq): NodeSeq = {
    xs.flatMap( i =>
      (
        ".awaiting:item [id]" #> idOf(i._3.id) &
        awaitingMessage(i._3)
      ).apply( bindItem(in, i._1, i._2, None, "not-used") )
    )
  }

  def awaitingMessage(clone: Clone): CssSel = {
    if (clone.attempts > 2) {
      ".item-message *" #> <span><span class="label warning">OH NO</span>{" Attempted " + clone.attempts + "x without success. Check sink disk usage."}</span>
    } else {
      ".item-message" #> NodeSeq.Empty
    }
  }
}
