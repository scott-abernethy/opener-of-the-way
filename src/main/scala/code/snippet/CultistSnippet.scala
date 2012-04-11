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
import code.comet.GatewayServer
import java.lang.String

class Cultist extends Loggable with CultistWho {
  val emailHint = "gone@insane.yet"
  object email extends RequestVar[Option[String]](Some(emailHint))
  object password extends RequestVar[Option[String]](None)
  object theRecruited extends RequestVar[Option[code.model.Cultist]](None)

  def recruit = {
    ClearClearable &
    ".recruit-email" #> JsCmds.FocusOnLoad(SHtml.text(email.is.getOrElse(""), t => email(Some(t))) % ("style" -> "width: 250px")) &
    "#recruit-submit" #> SHtml.submit("Submit", () => processRecruit) &
    "#recruit-cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }

  private def processRecruit {
    email.is match {
      case Some(Cultist.ValidEmail(username, domain)) if (domain.contains("aviat") || domain.contains("hstx.") || domain.contains("stratex")) => {
        S.warning("Don't be silly")
      }
      case Some(x) if (x == emailHint) => {
        S.warning("Don't be insane")
      }
      case Some(Cultist.ValidEmail(username, domain)) => {
        val e = username + "@" + domain
        val c = new code.model.Cultist
        c.email = e
        c.password = "beyond"
        c.recruitedBy = Cultist.attending.is.map(_.id) openOr -2L
        c.expired = true // forces password change
        c.locked = true // requires unlocking by the insane before they can glimpse the truth
        transaction {
          val free = cultists.where(_.email === c.email).isEmpty
          if (free) {
            cultists.insert(c)
          }
        }
        S.redirectTo("recruited", () => theRecruited(Some(c)))
      }
      case _ => {
        S.warning("Invalid email")
      }
    }
  }

  def recruited = {
    theRecruited.is match {
      case Some(c) => {
        ClearClearable &
        ".recruited-email" #> c.email &
        ".recruited-password" #> c.password
      }
      case _ => {
        S.redirectTo("/")
      }
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
      S.error("approach-messages", "Open your mouth, unworthy parrot!")
    } else {
      val c = Cultist.forEmail(submittedEmail)
      c.map( x => (x, x.approach(submittedPassword)) ).toOption match {
        case Some((cultist, ApproachSuccess)) => {
          S.notice("Proceed with care '" + cultist.sign + "'.")
          S.redirectTo("/", () => (Cultist.saveCookie))
        }
        case Some((cultist, ApproachExpired)) => {
          S.redirectTo("/cultist/expired", () => who(Some(cultist.id)))
        }
        case _ => {
          S.warning("approach-messages", "Unfumble your mind, unworthy worm!")
          logger.warn("Approach rejected for: '" + submittedEmail + "'")
          S.redirectTo("approach", () => email(Some(submittedEmail)))
        }
      }
    }
  }

  def withdraw = {
    Cultist.withdraw()
    S.redirectTo("approach", () => {
      Cultist.saveCookie
      S.notice("approach-messages", "Never return, or else...")
    })
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
    ".about:gateway [id]" #> ("g" + g.id) &
    ".gateway:state *" #> <span>{ GateState.symbol(g.state) } { g.state.toString }</span> &
    ".gateway:description *" #> g.description &
    ".gateway:mode *" #> g.modesDescription &
    ".gateway-edit" #> <a href={ "/gateway/edit?id=" + g.id }>Edit</a>
  }.apply(in)

  def removeGateway(gateway: code.model.Gateway): JsCmd = {
    transaction( Gateway.remove(gateway) )
    GatewayServer ! 'WayChanged
    JsCmds.Replace("g" + gateway.id, NodeSeq.Empty)
  }
}
