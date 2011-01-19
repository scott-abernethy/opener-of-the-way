package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import code.Db
import code.model._

import org.squeryl.PrimitiveTypeMode._

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    Db.init
    transaction {
      Mythos.drop
      Mythos.create
      val foo = Mythos.cultists.insert(new Cultist("foo@bar.com", "foo"))
      val two = Mythos.cultists.insert(new Cultist("two@bar.com", "two"))
      Mythos.gateways.insert(new Gateway(foo.id, "10.16.15.43/public", "foobar", "", "treesaregreen", GateMode.rw, GateState.lost))
      Mythos.gateways.insert(new Gateway(two.id, "10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.rw, GateState.lost))
    }

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
    S.addAround(new LoanWrapper(){ def apply[T](f : => T): T = transaction(f) })

    Environment.start
    LiftRules.unloadHooks.append(() => Environment.dispose)
  }
}
