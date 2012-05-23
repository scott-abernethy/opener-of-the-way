package code.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.{ListenerManager, CometActor, CometListener}
import net.liftweb.http.ListenerManager._
import net.liftweb.common.{Full, Loggable}
import org.squeryl.PrimitiveTypeMode._
import code.model.{Gateway, GateState, Cultist}
import xml.{Node, Unparsed, NodeSeq}
import net.liftweb.util.{CssSel, ClearClearable}

class GatewayInfo extends CometActor with CometListener {

  def registerWith = GatewayServer

  override def lowPriority = {
    // todo only listen to this cultists gateways
    case _ => reRender
  }

  def render = {
    // todo meh
    Cultist.attending.is match {
      case Full(cultist) =>
        val gateways = transaction ( cultist.gateways.toList )
        if (gateways.size > 0) {
          val t = ClearClearable &
          ".empty" #> NodeSeq.Empty &
          ".gateway-item" #> bindItems(gateways) _ &
          ".gateway-warning" #> bindWarnings(gateways) _
          if (gateways.filter(g => g.state == GateState.open || g.state == GateState.transient).isEmpty) {
            t & ".gateway-in-use [class+]" #> "hidden"
          }
          else {
            t
          }
        }
        else {
          ClearClearable &
          ".gateway-in-use [class+]" #> "hidden" &
          ".gateway-item" #> NodeSeq.Empty &
          ".gateway-warning" #> bindWarnings(gateways) _
        }
      case _ =>
        ClearClearable &
        ".gateway-in-use [class+]" #> "hidden" &
        ".gateway-item" #> NodeSeq.Empty &
        ".gateway-warning" #> NodeSeq.Empty
    }
  }
  
  def bindItems(gateways: List[Gateway])(in: NodeSeq): NodeSeq = {
    Cultist.attending.is match {
      case Full(cultist) =>
        gateways.flatMap(g =>
          (".gateway-mode *" #> g.modesIcon &
          ".gateway-status *" #> GateState.symbol(g.state) &
          ".gateway-description *" #> g.path).apply(in)
        )
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