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
//import model.Mythos._
//import gate.T
//
//
//import Helpers._
//import org.squeryl.PrimitiveTypeMode._
//import comet.{ChangedGateway, GatewayServer}
//
//class Gateway {
//  object share extends RequestVar("")
//  object path extends RequestVar("")
//  object password extends RequestVar("")
//  object mode extends RequestVar(Gateway.defaultMode)
//
//  def add = {
//    formFill(() => {
//      val g = new code.model.Gateway
//      processEdit(g)
//    })
//  }
//
//  private def formFill(submitAction: () => Unit) = {
//      val modeOptions = SHtml.radio(Gateway.encodeModeMap.keys.toList, Full(mode.is), x => mode(x))
//      ClearClearable &
//      ".add:share" #> JsCmds.FocusOnLoad(SHtml.text(share.is, t => share(t)) % ("style" -> "width: 250px")) &
//      ".add:path" #> (SHtml.text(path.is, t => path(t)) % ("style" -> "width: 250px")) &
//      ".add:password" #> (SHtml.password(password.is, t => password(t)) % ("style" -> "width: 250px")) &
//      ".add:mode" #> modeOptions.toForm &
//      "#add:submit" #> SHtml.submit("Submit", submitAction) &
//      "#add:cancel" #> SHtml.submit("Cancel", () => S.redirectTo("/cultist/profile"))
//    }
//
//  def edit = {
//    val editing: Option[code.model.Gateway] = for {
//      id <- S.param("id").toOption
//      key <- tryo(id.toLong)
//      gateway <- transaction( Mythos.gateways.lookup(key) )
//      cultist <- Cultist.attending.is.toOption
//      if (gateway.cultistId == cultist.id)
//    }
//    yield gateway
//
//    editing match {
//      case Some(g) => {
//        share(g.location)
//        path(g.path)
//        password(g.password)
//        mode(Gateway.decodeModeMap.get( (g.source, g.sink) ).getOrElse(Gateway.defaultMode))
//        formFill(() => processEdit(g))
//      }
//      case _ => {
//        S.error("No such gateway!")
//        S.redirectTo("/cultist/profile")
//      }
//    }
//  }
//
//  private def processEdit(g: code.model.Gateway) {
//    def save(cultist: code.model.Cultist) {
//      g.cultistId = cultist.id
//      g.location = share.is.trim
//      g.path = path.is.trim
//      g.password = password.is.trim
//      val (source, sink) = Gateway.encodeModeMap.get(mode.is) getOrElse (false, false)
//      g.source = source
//      g.sink = sink
//      transaction(gateways.insertOrUpdate(g))
//      GatewayServer ! ChangedGateway(g.id, cultist.id)
//      S.redirectTo("/cultist/profile")
//    }
//    // allow edits? with a big warning about implications, redetection etc
//    Cultist.attending.is match {
//      case Full(c) => share.is.trim match {
//        case Gateway.SmbProtocol(_, _) => save(c)
//        case Gateway.NfsProtocol(_, _) => save(c)
//        case _ => S.error("Invalid share definition! Please enter a valid share definition. (See examples for valid format).")
//      }
//      case _ => S.error("?!")
//    }
//  }
//}
