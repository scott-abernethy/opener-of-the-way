package code.model

import net.liftweb._
import http._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import mapper._

import net.liftweb.squerylrecord.SquerylRecord
import org.squeryl.adapters.H2Adapter

object Db {
  //import net.liftweb.squerylrecord.RecordTypeMode._
  def use[T](f : (SuperConnection) => T): T = DB.use(DefaultConnectionIdentifier)(f)
}
