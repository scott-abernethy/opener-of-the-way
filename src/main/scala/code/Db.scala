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

trait Db {
  lazy val driver = Props.get("db.driver") openOr "org.h2.Driver"
  lazy val adapter = Props.get("db.adapter") openOr "org.squeryl.adapters.H2Adapter"
  lazy val url = Props.get("db.url") openOr "jdbc:h2:test"
  lazy val user = Props.get("db.user") openOr ""
  lazy val password = Props.get("db.password") openOr ""
  def init {
    Class.forName(driver)
    val a: DatabaseAdapter = Class.forName(adapter).newInstance.asInstanceOf[DatabaseAdapter]
    SessionFactory.concreteFactory = Some(()=>
      Session.create(java.sql.DriverManager.getConnection(url, user, password), a)
    )
  }
  def close {}

  def describe { Mythos.printDdl }
}

object Db extends Db
