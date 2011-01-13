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
  object email extends RequestVar(emailHint)
  def join = {
    ClearClearable &
    ".join:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is, t => email(t)) % ("style" -> "width: 250px")) &
    "#join:submit" #> SHtml.submit("Submit", () => processJoin) &
    "#join:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }
  private def processJoin {
    import code.model.Cultist
    import code.model.Mythos._
    val c = new code.model.Cultist
    c.email = email.is
    cultists.insert(c)
    S.redirectTo("/")
  }
  def approach = {
    ClearClearable &
    ".approach:email" #> JsCmds.FocusOnLoad(SHtml.text(email.is, t => email(t)) % ("style" -> "width: 250px")) &
    "#approach:submit" #> SHtml.submit("Submit", () => processApproach)
  }
  def processApproach {
    var submittedEmail = email.is.toLowerCase

    if (submittedEmail == emailHint) {
      S.error("Please enter YOUR email address")
    } else {
      Cultist.forEmail(submittedEmail) match {
        case Full(c) =>
          Cultist.approach(c)
          S.notice("Proceed with care " + c.description)
          S.redirectTo("/", () => (Cultist.saveCookie))
        case Empty =>
          S.warning("'" + submittedEmail + "' is not yet worthy")
          S.redirectTo("join", () => email(submittedEmail))
        case Failure(msg, _, _) => S.error(msg)
      }
    }
  }
  def withdraw = {
    Cultist.withdraw()
    S.notice("Never return, or else")
    S.redirectTo("/", () => (Cultist.saveCookie))
  }
}
