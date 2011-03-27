package code

import model._
import org.squeryl.PrimitiveTypeMode._

object TestDb extends Db {
  import code.model.Mythos._
  override def init {
    super.init
    reset
  }
  def reset {
    transaction {
      Mythos.drop
      Mythos.create
    }
    transaction {
      val bob = cultists.insert(new Cultist("bob@bob.com", "bob123"))
      val jane = cultists.insert(new Cultist("jane@jane.com", "jane123"))

      val cow = gateways.insert(new Gateway(bob.id, "10.16.15.43/public", "frog/sheep/cow", "/tmp/cache/gate/cow", "cowsaregreen", GateMode.source, GateState.lost))
      val goat = gateways.insert(new Gateway(jane.id, "10.16.16.16/share", "goat.tc", "/tmp/cache/gate/goat", "nattyNAT", GateMode.sink, GateState.lost))

      val now = new java.sql.Timestamp(new java.util.Date().getTime)
      val glue = artifacts.insert(new Artifact(cow.id, "glue", now, now))
      val paper = artifacts.insert(new Artifact(cow.id, "stock/paper", now, now))
    }
  }
  lazy val c1: Cultist = cultists.lookup(1L) getOrElse null
  lazy val c2: Cultist = cultists.lookup(2L) getOrElse null
  lazy val c1g: Gateway = gateways.lookup(1L) getOrElse null
  lazy val c2g: Gateway = gateways.lookup(2L) getOrElse null
  lazy val c1ga1: Artifact = artifacts.lookup(1L) getOrElse null
  lazy val c1ga2: Artifact = artifacts.lookup(2L) getOrElse null
}
