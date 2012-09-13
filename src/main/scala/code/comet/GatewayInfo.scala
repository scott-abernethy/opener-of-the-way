package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http._
import js.JsCmds.SetElemById
import js.JsCmds.SetElemById
import net.liftweb.http.ListenerManager._
import net.liftweb.common.{Full, Loggable}
import org.squeryl.PrimitiveTypeMode._
import xml.{Text, Node, Unparsed, NodeSeq}
import net.liftweb.util.{ClearNodes, PassThru, CssSel, ClearClearable}
import code.model._
import js.{JE, JsCmds}
import net.liftweb.common.Full
import code.comet.ChangedGateway
import code.comet.ToState
import net.liftweb.common.Full
import code.comet.ChangedGateway
import code.comet.ToState
import code.gate.{Unlock, Lock, ScourAsap}
import code.util.DatePresentation

class GatewayInfo extends CometActor with CometListener with RegardToCultist {

  def registerWith = GatewayServer

  override def lowPriority = {
    case FlushAllGateways => {
      reRender()
    }
    case ToState(_, _, who) if (who == cultistId) => {
      reRender()
    }
    case ChangedGateway(_, who) if (who == cultistId) => {
      reRender()
    }
    case ChangedGateways(who) if (who == cultistId) => {
      reRender()
    }
    case _ => {}
  }

  def render = {
    whoOption() match {
      case Some(cultist) =>
        val gateways = transaction ( cultist.gateways.toList )
        val someOpen = gateways.filter(g => g.state == GateState.open || g.state == GateState.transient).size > 0
        val locked = cultist.shut.isDefined
        if (gateways.size > 0) {
          ClearClearable &
          ".empty" #> NodeSeq.Empty &
          ".coffin [class+]" #> (if (someOpen) Some("coffin-red") else (if (locked) Some("coffin-green") else None)) &
          ".coffin-top [class+]" #> (if (someOpen) Some("coffin-top-red") else (if (locked) Some("coffin-top-green") else None)) &
          ".gateway-in-use" #> (if (someOpen) PassThru else ClearNodes) &
          ".gateway-locked" #> (if (locked && !someOpen) (".gateway-locked-expire" #> (cultist.shut.map(t => DatePresentation.atAbbreviation(t.getTime)))) else ClearNodes) &
          "#gateway-lock *" #> (if (locked && !someOpen) "Unlock" else "Shut&Lock") &
          "#gateway-lock [title]" #> (if (locked && !someOpen) "Unlock now" else "Shut and lock shut all of your gateways for a period of time") &
          "#gateway-lock [disabled]" #> (if (locked && someOpen) Some("disabled") else None) &
          "#gateway-lock [onclick]" #> SHtml.ajaxInvoke(() => {
            val msg = if (locked) Unlock(cultist.id) else Lock(cultist.id)
            Environment.watcher ! msg
            SetElemById("gateway-lock", JE.Str("disabled"), "disabled")
          }) &
          ".gateway-item" #> bindItems(gateways) _ &
          ".gateway-warning" #> bindWarnings(gateways) _
        }
        else {
          ClearClearable &
          ".gateway-lock" #> NodeSeq.Empty &
          ".gateway-in-use" #> NodeSeq.Empty &
          ".gateway-item" #> NodeSeq.Empty &
          ".gateway-warning" #> bindWarnings(gateways) _
        }
      case _ =>
        ClearClearable &
        ".gateway-lock" #> NodeSeq.Empty &
        ".gateway-in-use" #> NodeSeq.Empty &
        ".gateway-item" #> NodeSeq.Empty &
        ".gateway-warning" #> NodeSeq.Empty
    }
  }
  
  def bindItems(gateways: List[Gateway])(in: NodeSeq): NodeSeq = {
    Cultist.attending.is match {
      case Full(cultist) =>
        gateways.flatMap{g =>
          val gwsId = "gws" + g.id
          (".gateway-mode" #> g.modesIcon &
          ".gateway-status" #> GateState.symbol(g.state) &
          ".gateway-title" #> Text(g.path + " ("+Gateway.decode(g.source,g.sink)+")")  &
          ".gateway-description" #> (if (g.source) Text("Last scoured: " + DatePresentation.atAbbreviation(g.seen.getTime)) else NodeSeq.Empty) &
          ".gateway-scour" #> (if (g.source) PassThru else ClearNodes) &
          ".gateway-scour [disabled]" #> (if (g.scourAsap) Some("disabled") else None) &
          ".gateway-scour [id]" #> gwsId &
          ".gateway-scour [onclick]" #> SHtml.ajaxInvoke(() => {
            Environment.watcher ! ScourAsap(g.id, cultist.id)
            SetElemById(gwsId, JE.Str("disabled"), "disabled")
          })
          ).apply(in)
        }
      case _ =>
        NodeSeq.Empty
    }
  }

  def bindWarnings(gateways: List[Gateway])(in: NodeSeq): NodeSeq = {
    val sources = gateways.filter(_.source).size
    val sinks = gateways.filter(_.sink).size
    val dual = gateways.filter(x => x.source && x.sink).size

    // Multiple sources. Odd.
    // Multiple sinks. Won't work.

    var warnings = List.empty[(Node, Node)]
    if (sources > 1) {
      warnings = (Gateway.symbolQuestion, <span>You have multiple source gateways, which is abnormal but acceptable.</span>) :: warnings
    } else if (sources == 0) {
      warnings = (Gateway.symbolQuestion, <span>You do not have a source gateway, thus you can not profer artifacts to fellow cultists. <a href="/gateway/add">Add Gateway</a></span>) :: warnings
    }
    if (sinks > 1) {
          warnings = (Gateway.symbolExclamation, <span>You have multiple sink gateways; only one will accept clones. <a href="/cultist/profile">Manage Gateways</a></span>) :: warnings
    } else if (sinks == 0) {
      warnings = (Gateway.symbolWarning, <span>You do not have a sink gateway, which means you can not clone artifacts - any artifacts you do select for cloning will stay awaiting until a sink is created. <a href="/gateway/add">Add Gateway</a></span>) :: warnings
    }
//    if (dual > 0) {
//      warnings = (Gateway.symbolExclamation, <span>One or more of your gateways is configured to dual source + sink mode, which is still under beta testing. Prepare for possible corruption! <a href="/cultist/profile">Manage Gateways</a></span>) :: warnings
//    }

    warnings.flatMap(w =>
      (
        ".warning-symbol *" #> w._1 &
        ".warning-text *" #> w._2
      ) apply(in)
    )
  }
}

object GatewayServer extends LiftActor with ListenerManager with Loggable {

  var createUpdate: AnyRef = "ignore"

  override def lowPriority = {
    case msg =>
      updateListeners(msg)
  }
}

sealed abstract class GatewayChange

case object FlushAllGateways
case class ToState(state: GateState.Value, gatewayId: Long, cultistId: Long)
case class ChangedGateway(gatewayId: Long, cultistId: Long)
case class ChangedGateways(cultistId: Long)