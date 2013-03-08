package model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import java.sql.Timestamp
import xml.{NodeSeq, Unparsed, Node}
import gate.{Millis, T}
import scala.util.matching.Regex
import play.api.libs.json.{JsBoolean, Json}
import concurrent.Future
import util.FutureTransaction._

class Gateway extends MythosObject {
  var cultistId: Long = 0
  var location: String = "" // hostname/sharename
  var path: String = "" // folder/subfolder/tcfilename
  var localPath: String = "" // /folder/subfolder
  var password: String = "" // storing in cleartext as none should have access to db
  var source: Boolean = true
  var sink: Boolean = true
  var state: GateState.Value = GateState.lost
  var stateDesc: String = ""
  var scoured: Timestamp = T.yesterday
  var scourAsap: Boolean = false
  var seen: Timestamp = T.yesterday // aka opened
  var requested: Timestamp = T.yesterday
  var failed: Timestamp = T.ago(Millis.days(2))
  var fails: Long = 0L
  var failCode: Long = 0L

  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToGateways.right(this)
  lazy val artifacts: OneToMany[Artifact] = Mythos.gatewayToArtifacts.left(this)

  def clonesPath: String = new File(localPath, "clones").getPath

  def mode: String = {
    Gateway.decode(source, sink)
  }

  def locationAndPath() = location + "/" + path

  override def toString = "Gateway[" + locationAndPath + "=" + source + sink + "]"

  def toJson = {
    val obj = Json.obj(
      "id" -> id,
      "uri" -> location,
      "path" -> path,
      "open" -> (state == GateState.open || state == GateState.transient),
      "mode" -> Json.obj(
        "name" -> Gateway.decode(source, sink),
        "source" -> source,
        "sink" -> sink
      ),
      "icon" -> (if (state == GateState.open || state == GateState.transient) "icon-folder-open" else "icon-folder-close-alt"),
      "class" -> (state match {
        case GateState.open => "s-open"
        case GateState.transient => "s-transient"
        case GateState.lost => "s-lost"
        case _ => ""
      })
    )
    if (source) {
      obj + ("scourAsap" -> JsBoolean(scourAsap))
    }
    else {
      obj
    }
  }
}

object Gateway {
  val SmbProtocol: Regex = """smb://([a-zA-Z0-9-._~!$&'()*+,;=]+)/([a-zA-Z0-9-._~!$&'()*+,;=:/]+)""".r
  val NfsProtocol: Regex = """nfs://([a-zA-Z0-9-._~!$&'()*+,;=]+):(/?[a-zA-Z0-9-._~!$&'()*+,;=:/]+)""".r

  lazy val viableDestinations: Query[Gateway] = {
    gateways.where(g =>
      g.sink === true and
      g.state === GateState.open)
  }

  def remove(gateway: Gateway) {
    Mythos.gateways.deleteWhere(x => x.id === gateway.id and x.source === false)
  }

  def save(gateway: Gateway) {
    transaction( gateways.insertOrUpdate(gateway) )
  }

  lazy val scourPeriod = Millis.hours(4)
  lazy val reopenTestAfter = Millis.minutes(30)
  lazy val rerequestableAfter = Millis.minutes(15)
  lazy val retryFailedAfter = Millis.minutes(60)

  lazy val symbolQuestion = <img src="/static/g_help.png" title="Did this slip your mind?"/>
  lazy val symbolWarning = <img src="/static/g_exclamation_lesser.png" title="Are you sure?!"/>
  lazy val symbolExclamation = <img src="/static/g_exclamation.png" title="Are you crazy?!!"/>

  val defaultMode = "Sink"

  val encodeModeMap = Map(
    "Source" -> (true, false),
    "Sink" -> (false, true),
    "Source + Sink" -> (true, true),
    "Disabled" -> (false, false)
  )

  val decodeModeMap = Map(
    (true, false) -> "Source",
    (false, true) -> "Sink",
    (true, true) -> "Source + Sink",
    (false, false) -> "Disabled"
  )

  def decode(source: Boolean, sink: Boolean): String = {
    decodeModeMap.get( (source, sink) ).getOrElse(Gateway.defaultMode)
  }

  def find(id: Long): Option[Gateway] = {
    inTransaction( gateways.lookup(id) )
  }

  def forCultist(id: Long): List[Gateway] = {
    inTransaction( from(gateways)(g => where(g.cultistId === id) select(g)).toList )
  }

  def sourceReport(): Future[List[(Gateway, String, String)]] = {
    inFutureTransaction(
      join( gateways, pseudonyms, cultists )( (g, n, w) =>
        where( g.source === true )
        select( (g, n.name, w.email) )
        orderBy( g.scoured asc, g.id asc )
        on( g.cultistId === n.id, g.cultistId === w.id )
      ).toList
    )
  }
}

object GateState extends Enumeration {
  type GateState = Value

  val open = Value("Open")
  val transient = Value("Transient")
  val closed = Value("Closed")
  val lost = Value("Lost")

  // TODO should the titles for these images give more help?
  def symbol(s: GateState.Value): Node = s match {
    case GateState.open => <img src="/static/g_open.png" title="Open"/>
    case GateState.transient => <img src="/static/g_transient.png" title="Open, closing..."/>
    case GateState.closed => <img src="/static/g_inactive.png" title="Closed"/>
    case GateState.lost => <img src="/static/g_lost.png" title="Lost"/>
    case _ => Unparsed("&nbsp;")
  }
}
