package code.snippet

import _root_.scala.xml.{NodeSeq, Text}
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.js._
import _root_.java.util.Date
import code.model._
import Helpers._
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._

class Cultist {
  val emailHint = "gone@insane.yet"
  object email extends RequestVar[Option[String]](Some(emailHint))
  object password extends RequestVar[Option[String]](None)

  def join = {
    ClearClearable &
    ".join:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is.getOrElse(""), t => email(Some(t))) % ("style" -> "width: 250px")) &
    "#join:submit" #> SHtml.submit("Submit", () => processJoin) &
    "#join:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }

  private def processJoin {
    email.is.filter(! _.contains("aviat")) match {
      case Some(e) =>
        //val c = new code.model.Cultist
        //c.email = e
        //cultists.insert(c)
        S.redirectTo("approach", () => email(Some(e)))
      case _ =>
        S.warning("Invalid email")
        S.redirectTo("join")
    }
  }

  def approach = {
    ClearClearable &
    ".approach:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is.getOrElse(""), t => email(Some(t))) % ("style" -> "width: 250px")) &
    ".approach:password" #> (SHtml.password("", t => password(Some(t))) % ("style" -> "width: 250px")) &
    "#approach:submit" #> SHtml.submit("Submit", () => processApproach)
  }

  def processApproach {
    var submittedEmail = email.is.map(_.toLowerCase).getOrElse("")
    var submittedPassword = password.is.getOrElse("")

    if (submittedEmail == emailHint) {
      S.error("Open your mouth, unworthy parrot!")
    } else { 
      Cultist.forEmail(submittedEmail).toOption.flatMap(Cultist.approach(_, submittedPassword)) match {
        case Some(c) =>
          S.notice("Proceed with care '" + c.sign + "'.")
          S.redirectTo("/", () => (Cultist.saveCookie))
        case _ =>
          S.warning("Unfumble your mind, unworthy worm!")
          S.redirectTo("approach", () => email(Some(submittedEmail)))
      }
    }
  }

  def withdraw = {
    Cultist.withdraw()
    S.notice("Never return, or else...")
    S.redirectTo("approach", () => (Cultist.saveCookie))
  }

  def profile = {
    Cultist.attending.is match {
      case Full(c) =>
        val gs = inTransaction(c.gateways.toList)
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
    ".gateway:state *" #> <span>{ GateState.symbol(g.state) } { g.state.toString }</span> &
    ".gateway:description *" #> g.description &
    ".gateway:mode *" #> <span>{ GateMode.symbol(g.mode) } { g.mode.toString }</span>
  }.apply(in)
}
