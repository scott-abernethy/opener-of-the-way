package code.comet

import net.liftweb._
import common._
import http._
import actor._
import js._
import js.JsCmds.{Replace, SetHtml}
import util._
import Helpers._
import code.model._
import scala.xml._
import org.squeryl.PrimitiveTypeMode._
import code.state.{ArtifactPack, ArtifactTouched, ArtifactServer}
import code.gate.T
import code.snippet.searchText

case class SearchInput(text: String)

class ArtifactSearch extends CometActor with CometListener with ArtifactBinding {
  var searchFor = ""
  var items: Seq[(Artifact, Option[ArtifactState.Value], Option[Int])] = Nil

  lazy val cultistId: Long = Cultist.attending.is.map(_.id).getOrElse(-1L)

  def registerWith = ArtifactServer

  override def lowPriority = {
    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
      partialUpdate(
        packUpdate((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("search:item")), cultistId, pack, ".search:item [id]")
      )
    }
    case SearchInput(text) => {
      searchFor = text
      items = new ArtifactCloneSearchFactory().create(cultistId, searchFor)
      this ! 'Publish
    }
    case 'Publish => {
      partialUpdate(Replace("searchx", renderResults.apply(defaultHtml)))
    }
    case _ => {}
  }

  def render = {
    this ! 'Publish
    ClearClearable &
    ".search-desc *" #> <span>Searching... <img src="/static/img/search-loader.gif"/></span> &
    ".search:item" #> NodeSeq.Empty
  }

  def renderResults = {
    if (items.size > 0) {
      ClearClearable &
        ".search-desc *" #> ("Found " + items.size + " matches for '" + searchFor + "'") &
        ".search:item" #> (bindItems(items) _)
    } else {
      ClearClearable &
        ".search-desc *" #> ("Found nothing matching '" + searchFor + "'") &
        ".search:item" #> NodeSeq.Empty
    }
  }

  def bindItems(items: Seq[(Artifact, Option[ArtifactState.Value], Option[Int])])(in: NodeSeq): NodeSeq = {
    items.flatMap(i => bindItem(in, i._1, i._2, i._3, ".search:item [id]"))
  }

  override def idFor(id: Long) = "sa" + id
}