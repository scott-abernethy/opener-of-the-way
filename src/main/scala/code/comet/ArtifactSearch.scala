package code.comet

import net.liftweb._
import common._
import http._
import actor._
import js._
import util._
import Helpers._
import code.model._
import scala.xml._
import org.squeryl.PrimitiveTypeMode._
import code.state.{ArtifactPack, ArtifactTouched, ArtifactServer}
import code.gate.T

case class SearchInput(text: String)

class ArtifactSearch extends CometActor with CometListener with ArtifactBinding {
  var searchFor: String = ""

  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1L)

  def registerWith = ArtifactServer

  override def lowPriority = {
    case ArtifactPack(change, artifact, ownerId, presence, clones) => {
      partialUpdate(packUpdate((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item")), artifact, cultistId, ownerId, presence, clones))
    }
    case SearchInput(text) => {
      searchFor = text
      reRender()
    }
    case _ => {}
  }

  def render = {
    val items = new ArtifactCloneSearchFactory().create(cultistId, searchFor)
    if (items.size > 0) {
      ClearClearable &
        ".search-desc *" #> ("Found " + items.size + " matches for '" + searchFor + "'") &
        ".log:item" #> (bindItems(items) _)
    } else {
      ClearClearable &
        ".search-desc *" #> ("Found nothing matching '" + searchFor + "'") &
        ".log:item" #> NodeSeq.Empty
    }
  }
}