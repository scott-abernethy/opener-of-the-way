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
    case pack @ ArtifactPack(change, artifact, ownerId, presence, clones) => {
      partialUpdate(packUpdate((defaultHtml \\ "div").filter(x => (x \ "@class").text.contains("log:item")), cultistId, pack, ".search:item [id]"))
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
        ".search:item" #> (bindItems(items) _)
    } else {
      ClearClearable &
        ".search-desc *" #> ("Found nothing matching '" + searchFor + "'") &
        ".search:item" #> NodeSeq.Empty
    }
  }

  def bindItems(items: Seq[(Artifact, Option[ArtifactState.Value])])(in: NodeSeq): NodeSeq = {
    items.flatMap(i => bindItem(in, i._1, i._2, None, ".search:item [id]"))
  }

  override def idFor(id: Long) = "sa" + id
}