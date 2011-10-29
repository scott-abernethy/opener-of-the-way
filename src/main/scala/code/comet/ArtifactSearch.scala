package code.comet

import net.liftweb._
import common._
import http._
import http.js._
import actor._
import util._
import Helpers._
import code.model._
import scala.xml._
import org.squeryl.PrimitiveTypeMode._

case class SearchInput(text: String)

class ArtifactSearch extends CometActor with CometListener with ArtifactBinding {
  var searchFor: String = ""
  def registerWith = ArtifactServer
  override def lowPriority = {
    case ArtifactUpdated(a) =>
      reRender()
    case ArtifactCloned(a) =>
      reRender()
    case SearchInput(text) =>
      searchFor = text
      reRender()
    case _ =>
  }
  def render = {
    val items = new ArtifactCloneSearchFactory().create(Cultist.attending.is.map(_.id).getOrElse(-1), searchFor)
    if (items.size > 0) {
      ClearClearable &
        ".search-desc *" #> ("Found " + items.size + " matches") &
        ".search:item" #> (bindItems(items) _)
    } else {
      ClearClearable &
        ".search-desc *" #> "Found nothing" &
        ".search:item" #> NodeSeq.Empty
    }
  }
}