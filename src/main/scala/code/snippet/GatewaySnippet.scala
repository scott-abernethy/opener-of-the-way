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
  object host extends RequestVar("")
  object share extends RequestVar("")
  object path extends RequestVar("")
  object password extends RequestVar("")
  object mode extends RequestVar(Gateway.defaultMode)

  def add = {
    formFill(() => {
      val g = new code.model.Gateway
      processEdit(g)
    })
  }

  private def formFill(submitAction: () => Unit) = {
      val modeOptions = SHtml.radio(Gateway.encodeModeMap.keys.toList, Full(mode.is), x => mode(x))
      ClearClearable &
      ".add:host" #> JsCmds.FocusOnLoad(SHtml.text(host.is, t => host(t)) % ("style" -> "width: 250px")) &
      ".add:share" #> (SHtml.text(share.is, t => share(t)) % ("style" -> "width: 250px")) &
      ".add:path" #> (SHtml.text(path.is, t => path(t)) % ("style" -> "width: 250px")) &
      ".add:password" #> (SHtml.password(password.is, t => password(t)) % ("style" -> "width: 250px")) &
      ".add:mode" #> modeOptions.toForm &
      "#add:submit" #> SHtml.submit("Submit", submitAction) &
      "#add:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/cultist/profile"))
    }

  def edit = {
    val editing: Option[code.model.Gateway] = for {
      id <- S.param("id").toOption
      key <- tryo(id.toLong)
      gateway <- transaction( Mythos.gateways.lookup(key) )
      cultist <- Cultist.attending.is.toOption
      if (gateway.cultistId == cultist.id)
    }
    yield gateway

    editing match {
      case Some(g) => {
        val split = g.location.lastIndexOf("/")
        host(g.location.substring(0, split))
        share(g.location.substring(split + 1))
        path(g.path)
        password(g.password)
        mode(Gateway.decodeModeMap.get( (g.source, g.sink) ).getOrElse(Gateway.defaultMode))
        formFill(() => processEdit(g))
      }
      case _ => {
        S.error("No such gateway!")
        S.redirectTo("/cultist/profile")
      }
    }
  }

  private def processEdit(g: code.model.Gateway) {
    Cultist.attending.is match {
      case Full(c) =>
        // replace \ with /
        // check host matches a pattern
        // allow edits? with a big warning about implications, redetection etc
        g.cultistId = c.id
        g.location = host.is.trim + "/" + share.is.trim
        g.path = path.is.trim
        g.password = password.is.trim
        val (source, sink) = Gateway.encodeModeMap.get(mode.is) getOrElse (false, false)
        g.source = source
        g.sink = sink
        transaction(gateways.insertOrUpdate(g))
        GatewayServer ! 'WayChanged
      case _ => S.error("?!")
    }
    S.redirectTo("/cultist/profile")
  }
}
