package code

import net.liftweb._
import http._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import mapper._

import net.liftweb.squerylrecord.SquerylRecord
import org.squeryl.adapters.H2Adapter

import code.model.Mythos

object TestDb {
  //import net.liftweb.squerylrecord.RecordTypeMode._
  val vendor = new StandardDBVendor("org.h2.Driver", "jdbc:h2:mem:test", Empty, Empty)
  def open() {
    try {
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
      SquerylRecord.init(() => new H2Adapter)

      use{ _ => 
        Mythos.create  
      }
    } catch {
      case _ => println("erg")
    }
  }
  def use[T](f : (SuperConnection) => T): T = DB.use(DefaultConnectionIdentifier)(f)
  def close() {
    vendor.closeAllConnections_!
  }
}
