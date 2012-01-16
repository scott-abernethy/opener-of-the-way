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

trait CultistWho {
  object who extends RequestVar[Option[Long]](None)  
}

class Expired extends Loggable with CultistWho {
  object currentPassword extends RequestVar[String]("")
  object newPassword extends RequestVar[String]("")
  object newPasswordAgain extends RequestVar[String]("")

  def changePassword = {
    val whoId: Option[Long] = who.is.orElse(Cultist.attending.is.toOption.map(_.id))
    ClearClearable &
    ".current-password" #> JsCmds.FocusOnLoad(SHtml.password(currentPassword.is, x => currentPassword(x))) &
    ".new-password" #> (SHtml.password(newPassword.is, x => newPassword(x))) &
    ".new-password-again" #> (SHtml.password(newPasswordAgain.is, x => newPasswordAgain(x))) &
    "#submit" #> SHtml.submit("Submit", () => { who(whoId); processChangePassword }) &
    "#cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }

  private def processChangePassword {
    transaction( who.is.flatMap(cultists.lookup(_)) ) match {
      case Some(cultist) if (currentPassword.is != cultist.password) => {
        S.error("current-password-messages", "Incorrect password!")
      }
      case Some(cultist) if (newPassword.is.length < 8) => {
        S.error("new-password-messages", "New password must be 8 characters or greater!") 
      }
      case Some(cultist) if (newPassword.is != newPasswordAgain.is) => {
        S.error("new-password-messages", "New passwords do not match!")
      }
      case Some(cultist) => {
        transaction{
          update(cultists)(c =>
            where(c.id === cultist.id)
            set(c.password := newPassword.is, c.expired := false)
          )
        }
        Cultist.attending(Empty)
        S.redirectTo("/cultist/approach", () => S.notice("approach-messages", "Password changed."))
      }
      case _ => {
        S.redirectTo("/")
      }
    }    
  }
}