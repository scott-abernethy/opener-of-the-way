package code.js

import java.util.UUID
import net.liftweb.http.S
import net.liftweb.util.AltXML
import net.liftweb.util.Helpers._
import net.liftweb.http.js._
import jquery.JqJE.JqId
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import xml.{Text, Group, NodeSeq}
import net.liftweb.http.SHtml._

object JqConfirmationDialog {
  def apply(title: String, message: String, onYes: () => JsCmd, onNo: () => JsCmd = () => JsCmds.Noop): JsCmd = {
    val close = JsRaw("$(this).dialog('close');")


    val yesMapped = S.fmapFunc(onYes)(name => makeAjaxCall(Str(name + "=true")))
    val noMapped = S.fmapFunc(onNo)(name => makeAjaxCall(Str(name + "=true")))
    val yesFunc = AnonFunc(close & yesMapped).toJsCmd
    val noFunc = AnonFunc(close & noMapped).toJsCmd
//    JsRaw("jQuery('<div></div>').html('<p>" + message + "</p>').dialog({ resizeable: false, modal: true, title: '" + title + "', buttons: { 'Yes': "+yesFunc+", 'No': "+noFunc+" } });").cmd
    JsRaw("if (confirm(" + message.encJs + ")) {" + yesMapped.toJsCmd + "} else {" + noMapped.toJsCmd + "}").cmd
  }
}

///**
// * @author Yaroslav Klymko
// */
//class JqDialog(title: String, xhtml: NodeSeq, options: (String, Any)*)
//extends JsCmd {
//  def formatHtml(node: NodeSeq) = AltXML.toXML(Group(
//    S.session.map(s => s.fixHtml(s.processSurroundAndInclude("JqDialog",
//xhtml))).openOr(xhtml)),
//    false, true, S.ieMode).encJs
//  def formatOptions(opts: (String, Any)*): String =
//"{%s}".format(opts.map(formatOption(_)).mkString(", "))
//  protected  def formatOption(opt: (String, Any)): String = opt match {
//    case (name, value: Seq[(String, Any)]) => formatOption(name,
//formatOptions(value: _*))
//    case (name, func: (() => JsCmd)) => formatOption(name, func())
//    case (name, func: (String => JsCmd)) => formatOption(name, func(id))
//    case (name, value: JsCmd) => formatOption(name, AnonFunc(value).toJsCmd)
//    case (name, value) => name + ": " + value.toString
//  }
//  val cssClass: String = JqDialog.prefix
//  val id = JqDialog.randomId
//  protected def js = "jQuery(" + (".format(id, cssClass, title)").encJs + ")" +
//    ".html(" + formatHtml(xhtml) + ")" +
//    ".dialog(" + formatOptions(options: _*) + ").dialog(" + ("'open'").encJs + ");"
////  protected def js = JqId(Str("errorDialog_id")) ~>  new JsExp with JsMember {
////    def toJsCmd = "dialog('open')"
////  }
//
//  lazy val toJsCmd = js
//  lazy val close = JqDialogClose(id)
//}
//
//object JqDialog {
//  val prefix = "jui-dialog"
//  def randomId: String = prefix + "-" + UUID.randomUUID
//}
//
//object JqDialogClose {
//  def apply(id: String): JsCmd = JsRaw("jQuery('#" + id + "').remove();").cmd
//  def apply(id: String, func: () => JsCmd): JsCmd = apply(id, func())
//  def apply(id: String, func: JsCmd): JsCmd = func & apply(id)
//}
//
//object JqConfirmationDialog {
//  def apply(title: String, message: String, onYes: () => JsCmd, onNo: () =>
//JsCmd = () => JsCmds.Noop) = {
//    val yesBtn: (String, (String => JsCmd)) = S.?("Yes") -> ((id: String) => JqDialogClose(id, onYes))
//    val noBtn: (String, (String => JsCmd)) = S.?("No") -> ((id: String) => JqDialogClose(id, onNo))
//    new JqDialog(title, Text(message), "modal" -> true, "buttons" -> Seq(yesBtn, noBtn))
//  }
//}