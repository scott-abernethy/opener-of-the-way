package code.snippet

import _root_.scala.xml.{NodeSeq, Text}
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.js._
import _root_.java.util.Date
import code.model._
import Helpers._
import org.squeryl.PrimitiveTypeMode._

class Gateway {
  val modes = GateMode.values.toSeq map (i => (i.toString, i.toString))
  object host extends RequestVar("")
  object share extends RequestVar("")
  object path extends RequestVar("")
  object password extends RequestVar("")
  object mode extends RequestVar(GateMode.source)
  def add = {
    ClearClearable &
    ".add:host" #> JsCmds.FocusOnLoad(SHtml.text(host.is, t => host(t)) % ("style" -> "width: 250px")) &
    ".add:share" #> (SHtml.text(share.is, t => share(t)) % ("style" -> "width: 250px")) &
    ".add:path" #> (SHtml.text(path.is, t => path(t)) % ("style" -> "width: 250px")) &
    ".add:password" #> (SHtml.password(password.is, t => password(t)) % ("style" -> "width: 250px")) &
    ".add:mode" #> (SHtml.select(modes, Full(mode.is.toString), (selected) => (mode(GateMode parse selected)))) &
    "#add:submit" #> SHtml.submit("Submit", () => processAdd) &
    "#add:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/"))
  }
  private def processAdd {
    import code.model.{Gateway, Cultist}
    import code.model.Mythos._
    Cultist.attending.is match {
      case Full(c) =>
        val g = gateways.insert(new code.model.Gateway(c.id, host.is.trim + "/" + share.is.trim, path.is.trim, "", password.is.trim, mode.is, GateState.lost))
        // TODO squeryl doesn't have lifecycle callbacks at present, so we must manually trigger event
        Environment.watch(g)
      case _ => S.error("?!")
    }
    S.redirectTo("/cultist/profile")
  }
}
