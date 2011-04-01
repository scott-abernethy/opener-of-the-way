package code

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import code.model._

import org.squeryl._
import internals.DatabaseAdapter
import org.squeryl.adapters.H2Adapter
import org.squeryl.adapters.MySQLAdapter

trait Db {
  lazy val driver = Props.get("db.driver") openOr "org.squeryl.adapters.H2Adapter"
  lazy val url = Props.get("db.url") openOr "jdbc:h2:test"
  lazy val user = Props.get("db.user") openOr ""
  lazy val password = Props.get("db.password") openOr ""
  def init {
    val d: DatabaseAdapter = Class.forName(driver).newInstance.asInstanceOf[DatabaseAdapter]
    SessionFactory.concreteFactory = Some(()=>
      Session.create(java.sql.DriverManager.getConnection(url, user, password), d)
    )
  }
  def close {}

  def describe { Mythos.printDdl }
}

object Db extends Db
