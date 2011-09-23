package code.snippet

import _root_.scala.xml.{NodeSeq, Text}
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.js._
import _root_.java.util.Date
import code.model._
import code.model.Mythos._
import code.gate.T


import Helpers._
import org.squeryl.PrimitiveTypeMode._
import code.comet.GatewayServer

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
    Cultist.attending.is match {
      case Full(c) =>
        // replace \ with /
        // check host matches a pattern
        // allow edits? with a big warning about implications, redetection etc
        val g = new code.model.Gateway
        g.cultistId = c.id
        g.location = host.is.trim + "/" + share.is.trim
        g.path = path.is.trim
        g.password = password.is.trim
        g.mode = mode.is
        transaction(gateways.insert(g))
        GatewayServer ! 'WayChanged
      case _ => S.error("?!")
    }
    S.redirectTo("/cultist/profile")
  }
}
