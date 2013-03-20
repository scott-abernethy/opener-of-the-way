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
//import xml.NodeSeq
//import _
//import _root_.net.liftweb.util._
//import _root_.net.liftweb.common._
//import _root_.net.liftweb.http._
//import _root_.net.liftweb.http.js._
//import Helpers._
//import comet.{ArtifactSearch, SearchInput, ArtifactBinding}
//
//object searchText extends RequestVar[Option[String]](None)
//
//class ArtifactSnippet extends ArtifactBinding {
//  def searcher = {
//    if (model.Cultist.attending_?) {
//      ClearClearable &
//      ".search:text" #> JsCmds.FocusOnLoad(SHtml.text(searchText.is.getOrElse(""), t => searchText(Some(t)), "placeholder" -> "Search")) &
//      "#search:submit" #> SHtml.submit("Search", () => processSearch)
//    } else {
//      "#searcher" #> NodeSeq.Empty
//    }
//  }
//
//  def processSearch {
//    val tmp = searchText.is.getOrElse("")
//    S.redirectTo("search", () => {
//      S.session.foreach(_.sendCometActorMessage("ArtifactSearch", Empty, SearchInput(tmp)))
//      searchText(Some(tmp))
//    })
//  }
//}