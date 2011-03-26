package code.snippet

import _root_.scala.xml.{NodeSeq, Text}
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.js._
import _root_.java.util.Date
import code.model._
import Helpers._

class Cultist {
  //def howdy = "#time *" #> date.map(_.toString)
  val emailHint = "gone@insane.yet"
  object email extends RequestVar[Option[String]](Some(emailHint))
  def join = {
    ClearClearable &
    ".join:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is.getOrElse(""), t => email(Some(t))) % ("style" -> "width: 250px")) &
    "#join:submit" #> SHtml.submit("Submit", () => processJoin) &
    "#join:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }
  private def processJoin {
    import code.model.Mythos._
    email.is.filter(! _.contains("aviat")) match {
      case Some(e) =>
        val c = new code.model.Cultist
        c.email = e
        cultists.insert(c)
        S.redirectTo("approach", () => email(Some(e)))
      case _ =>
        S.warning("Invalid email")
        S.redirectTo("join")
    }
  }
  def approach = {
    ClearClearable &
    ".approach:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is.getOrElse(""), t => email(Some(t))) % ("style" -> "width: 250px")) &
    "#approach:submit" #> SHtml.submit("Submit", () => processApproach)
  }
  def processApproach {
    var submittedEmail = email.is.map(_.toLowerCase).getOrElse("")

    if (submittedEmail == emailHint) {
      S.error("Please enter YOUR email address")
    } else { 
      Cultist.forEmail(submittedEmail) match {
        case Full(c) =>
          Cultist.approach(c)
          S.notice("Proceed with care " + c.sign)
          S.redirectTo("/", () => (Cultist.saveCookie))
        case Empty =>
          S.warning("'" + submittedEmail + "' is not yet worthy")
          //S.redirectTo("join", () => email(Some(submittedEmail)))
          S.redirectTo("approach", () => email(Some(submittedEmail)))
        case Failure(msg, _, _) => S.error(msg)
      }
    }
  }
  def withdraw = {
    Cultist.withdraw()
    S.notice("Never return, or else")
    S.redirectTo("/", () => (Cultist.saveCookie))
  }
  def profile = {
    Cultist.attending.is match {
      case Full(c) =>
        val gs = c.gateways.toSeq
        ClearClearable &
        ".about:sign *" #> c.sign &
        ".about:email *" #> c.email &
        ".about:gateway" #> bindGateways(gs) _
      case _ =>
        S.redirectTo("/")
    }
  }
  def bindGateways(gs: Seq[code.model.Gateway])(in: NodeSeq): NodeSeq = gs.flatMap(bindGateway(in, _))
  def bindGateway(in: NodeSeq, g: code.model.Gateway): NodeSeq = {
    ClearClearable &
    ".gateway:state" #> g.state.toString &
    ".gateway:description" #> g.description &
    ".gateway:mode" #> g.mode.toString
  }.apply(in)
}
