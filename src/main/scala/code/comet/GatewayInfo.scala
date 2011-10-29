package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{ListenerManager, CometActor, CometListener}
import net.liftweb.http.ListenerManager._
import net.liftweb.common.{Full, Loggable}
import org.squeryl.PrimitiveTypeMode._
import code.model.{Gateway, GateMode, GateState, Cultist}
import xml.{Node, Unparsed, NodeSeq}
import net.liftweb.util.{CssSel, ClearClearable}

class GatewayInfo extends CometActor with CometListener {

  def registerWith = GatewayServer

  override def lowPriority = {
    case _ => reRender
  }

  def render = {
    Cultist.attending.is match {
      case Full(cultist) =>
        val gateways = transaction ( cultist.gateways.toList )
        if (gateways.size > 0) {
          ClearClearable &
          ".empty" #> NodeSeq.Empty &
          ".gateway-item" #> bindItems(gateways) _ &
          ".gateway-warning" #> bindWarnings(gateways) _
        } else {
          ClearClearable &
          ".gateway-item" #> NodeSeq.Empty &
          ".gateway-warning" #> bindWarnings(gateways) _
        }
      case _ =>
        ClearClearable &
        ".gateway-item" #> NodeSeq.Empty &
        ".gateway-warning" #> NodeSeq.Empty
    }
  }
  
  def bindItems(gateways: List[Gateway])(in: NodeSeq): NodeSeq = {
    Cultist.attending.is match {
      case Full(cultist) =>
        gateways.flatMap(g =>
          (".gateway-mode *" #> GateMode.symbol(g.mode) &
          ".gateway-status *" #> GateState.symbol(g.state) &
          ".gateway-description *" #> g.path).apply(in)
        )
      case _ =>
        NodeSeq.Empty
    }
  }

  def bindWarnings(gateways: List[Gateway])(in: NodeSeq): NodeSeq = {
    val sources = gateways.filter(_.mode == GateMode.source).size
    val sinks = gateways.filter(_.mode == GateMode.sink).size

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