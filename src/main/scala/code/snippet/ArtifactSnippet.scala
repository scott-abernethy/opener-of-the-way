package code.snippet

import code.comet.ArtifactBinding
import xml.NodeSeq
import code.model.{ArtifactState, Artifact, Cultist, ArtifactCloneSearchFactory}
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.js._
import Helpers._

class ArtifactSnippet extends ArtifactBinding {
  object searchText extends RequestVar[Option[String]](None)
  def searcher = {
    ClearClearable &
      ".search:text" #> JsCmds.FocusOnLoad(SHtml.text(searchText.is.getOrElse(""), t => searchText(Some(t))) % ("style" -> "width: 250px")) &
      "#search:submit" #> SHtml.submit("Search", () => processSearch)
  }
  def processSearch {
    val tmp = searchText.is.getOrElse("")
    S.redirectTo("search", () => searchText(Some(tmp)))
  }
  def search = {
    val items = new ArtifactCloneSearchFactory().create(Cultist.attending.is.map(_.id).getOrElse(-1), searchText.is.getOrElse(""))
    ClearClearable &
      ".search:item" #> (bindItems(items) _)
  }
}