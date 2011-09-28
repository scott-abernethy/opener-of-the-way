package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import code.Db
import code.model._

import org.squeryl.PrimitiveTypeMode._
import net.liftweb.widgets.flot.Flot

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    val db = new Db{}
    db.init
    db.populate
    LiftRules.unloadHooks.append(() => db.close)

    // where to search snippet
    LiftRules.addToPackages("code")

    val isAttending = If(() => Cultist.attending_?, () => RedirectResponse("/cultist/approach"))
    val notAttending = Unless(() => Cultist.attending_?, () => RedirectResponse("/"))
    val isInsane = If(() => Cultist.attending.is.exists(_.insane), () => RedirectResponse("/cultist/approach"))
    
    // Build SiteMap
    def sitemap = SiteMap(
      Menu.i("Home") / "index" >> isAttending,
      Menu.i("Search") / "search" >> isAttending >> Hidden,
      Menu.i("Awaiting") / "awaiting" >> isAttending >> Hidden,
      Menu.i("Approach") / "cultist" / "approach" >> notAttending,
//      Menu.i("Join") / "cultist" / "join" >> notAttending,
      Menu.i("Profile") / "cultist" / "profile" >> isAttending,
      Menu.i("Tome") / "static" / "tome" >> isAttending,
      Menu.i("Observe") / "observe" >> isInsane,
      Menu.i("Withdraw") / "cultist" / "withdraw" >> isAttending,
      Menu.i("Add Gateway") / "gateway" / "add" >> Hidden >> isAttending)

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemap)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => false)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    LiftRules.noticesAutoFadeOut.default.set( (notices: NoticeType.Value) => {
      notices match {
        case NoticeType.Notice => Full((10 seconds, 2 seconds))
        case _ => Empty
      }
    } )

    Flot.init

    Environment.start
    LiftRules.unloadHooks.append(() => Environment.dispose)
  }
}
