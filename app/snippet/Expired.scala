/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//package snippet
//
//import _root_.scala.xml.{NodeSeq, Text}
//import _root_.net.liftweb.util._
//import _root_.net.liftweb.common._
//import _root_.net.liftweb.http._
//import _root_.net.liftweb.http.js._
//import _root_.java.util.Date
//import model._
//import Helpers._
//import model.Mythos._
//import org.squeryl.PrimitiveTypeMode._
//import comet.GatewayServer
//import java.lang.String
//
//trait CultistWho {
//  object who extends RequestVar[Option[Long]](None)
//}
//
//class Expired extends Loggable with CultistWho {
//  object currentPassword extends RequestVar[String]("")
//  object newPassword extends RequestVar[String]("")
//  object newPasswordAgain extends RequestVar[String]("")
//
//  def changePassword = {
//    val whoId: Option[Long] = who.is.orElse(Cultist.attending.is.toOption.map(_.id))
//    ClearClearable &
//    ".current-password" #> JsCmds.FocusOnLoad(SHtml.password(currentPassword.is, x => currentPassword(x))) &
//    ".new-password" #> (SHtml.password(newPassword.is, x => newPassword(x))) &
//    ".new-password-again" #> (SHtml.password(newPasswordAgain.is, x => newPasswordAgain(x))) &
//    "#submit" #> SHtml.submit("Submit", () => { who(whoId); processChangePassword }) &
//    "#cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
//  }
//
//  private def processChangePassword {
//    transaction( who.is.flatMap(cultists.lookup(_)) ) match {
//      case Some(cultist) if (currentPassword.is != cultist.password) => {
//        S.error("current-password-messages", "Incorrect password!")
//      }
//      case Some(cultist) if (newPassword.is.length < 8) => {
//        S.error("new-password-messages", "New password must be 8 characters or greater!")
//      }
//      case Some(cultist) if (newPassword.is != newPasswordAgain.is) => {
//        S.error("new-password-messages", "New passwords do not match!")
//      }
//      case Some(cultist) => {
//        transaction{
//          update(cultists)(c =>
//            where(c.id === cultist.id)
//            set(c.password := newPassword.is, c.expired := false)
//          )
//        }
//        Cultist.attending(Empty)
//        S.redirectTo("/cultist/approach", () => S.notice("approach-messages", "Password changed."))
//      }
//      case _ => {
//        S.redirectTo("/")
//      }
//    }
//  }
//}