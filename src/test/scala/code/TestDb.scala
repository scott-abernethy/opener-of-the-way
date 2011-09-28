package code

import model._
import org.squeryl.PrimitiveTypeMode._

class TestDb extends Db {
  import code.gate.T
  import code.model.Mythos._
  def reset {
    clear
    transaction {
      val bob = cultists.insert(Cultist.create("bob@bob.com", "bob123"))
      val jane = cultists.insert(Cultist.create("jane@jane.com", "jane123"))
      val chew = cultists.insert(Cultist.create("chew@chew.com", "chew123"))

      val g1: Gateway = new Gateway
      g1.cultistId = bob.id
      g1.location = "10.16.15.43/public"
      g1.path = "frog/sheep/cow"
      g1.localPath = "/tmp/cache/gate/cow"
      g1.password = "cowsaregreen"
      g1.mode = GateMode.source
      val g2: Gateway = new Gateway
      g2.cultistId = jane.id
      g2.location = "10.16.16.16/share"
      g2.path = "goat.tc"
      g2.localPath = "/tmp/cache/gate/goat"
      g2.password = "nattyNAT"
      g2.mode = GateMode.sink
      val g3: Gateway = new Gateway
      g3.cultistId = chew.id
      g3.location = "1.2.3.4/p"
      g3.path = "file"
      g3.localPath = "/tmp/cache/gate/sheep"
      g3.password = "baaaa"
      g3.mode = GateMode.sink

      val cow = gateways.insert(g1)
      val goat = gateways.insert(g2)
      val sheep = gateways.insert(g3)

      val now = new java.sql.Timestamp(new java.util.Date().getTime)
      val glue = artifacts.insert(new Artifact(cow.id, "glue", T.ago(15*60*1000), now))
      val paper = artifacts.insert(new Artifact(cow.id, "stock foo/paper part p", T.ago(15*60*1000), now))
    }
  }
  lazy val c1: Cultist = cultists.lookup(1L) getOrElse null
  lazy val c2: Cultist = cultists.lookup(2L) getOrElse null
  lazy val c3: Cultist = cultists.lookup(3L) getOrElse null
  lazy val c1g: Gateway = gateways.lookup(1L) getOrElse null
  lazy val c2g: Gateway = gateways.lookup(2L) getOrElse null
  lazy val c3g: Gateway = gateways.lookup(3L) getOrElse null
  lazy val c1ga1: Artifact = artifacts.lookup(1L) getOrElse null
  lazy val c1ga2: Artifact = artifacts.lookup(2L) getOrElse null
}
