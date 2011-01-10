package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import code.model._

import net.liftweb.squerylrecord.SquerylRecord
import org.squeryl.adapters.H2Adapter

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr "jdbc:h2:test",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    SquerylRecord.init(() => new H2Adapter)

    //DB.use(DefaultConnectionIdentifier)(_ => code.model.Mythos.create)

    // where to search snippet
    LiftRules.addToPackages("code")

    val isAttending = If(() => Cultist.attending_?, () => RedirectResponse("/cultist/approach"))
    val notAttending = Unless(() => Cultist.attending_?, () => RedirectResponse("/"))

    // Build SiteMap
    def sitemap = SiteMap(Menu.i("Home") / "index" >> isAttending,
      Menu.i("Join") / "cultist" / "join" >> notAttending,
      Menu.i("Approach") / "cultist" / "approach" >> notAttending,
      Menu.i("Withdraw") / "cultist" / "withdraw" >> isAttending,
      Menu.i("Add Gateway") / "gateway" / "add" >> isAttending)

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemap)

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

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    Environment.start
    LiftRules.unloadHooks.append(() => Environment.dispose)
  }
}
