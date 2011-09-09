package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.gate.T
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import java.sql.Timestamp

class Gateway(
  var cultistId: Long,
  var location: String, // hostname/sharename
  var path: String, // folder/subfolder/tcfilename
  var localPath: String, // /folder/subfolder
  var password: String, // storing in cleartext as none should have access to db
  var mode: GateMode.Value,
  var state: GateState.Value,
  var stateDesc: String,
  var scoured: Timestamp
) extends MythosObject {
  def this() = this(0, "", "", "", "", GateMode.source, GateState.lost, "", T.yesterday)
  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToGateways.right(this)
  lazy val artifacts: OneToMany[Artifact] = Mythos.gatewayToArtifacts.left(this)
  def clonesPath: String = new File(localPath, "clones").getPath
  def description: String = new File(location, path).getPath
}

object Gateway {
  lazy val viableDestinations: Query[Gateway] = gateways.where(g => g.mode === GateMode.sink and g.state === GateState.open)
}

object GateMode extends Enumeration {
  type GateMode = Value
  val source = Value("source (read only)")
  val sink = Value("sink (write only)")
  def parse(text: String): GateMode = text match {
    case "sink (write only)" => sink
    case _ => source
  }  
}

object GateState extends Enumeration {
  type GateState = Value
  val open = Value("open")
  val inactive = Value("closed")
  val lost = Value("lost")
}
