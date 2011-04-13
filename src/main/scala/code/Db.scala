package code

import net.liftweb._
import net.liftweb.util.Props

import code.model._

import org.squeryl._
import internals.DatabaseAdapter
import org.squeryl.PrimitiveTypeMode._
import com.mchange.v2.c3p0.ComboPooledDataSource

trait Db {
  lazy val driver = Props.get("db.driver") openOr "org.h2.Driver"
  lazy val adapter = Props.get("db.adapter") openOr "org.squeryl.adapters.H2Adapter"
  lazy val url = Props.get("db.url") openOr "jdbc:h2:test"
  lazy val user = Props.get("db.user") openOr ""
  lazy val password = Props.get("db.password") openOr ""
  def init {
    Class.forName(driver)
    val a: DatabaseAdapter = Class.forName(adapter).newInstance.asInstanceOf[DatabaseAdapter]

    // Setup connection pooling with c3p0
    val pool = new ComboPooledDataSource
    pool.setDriverClass(driver)
    pool.setJdbcUrl(url)
    pool.setUser(user)
    pool.setPassword(password)
    pool.setMinPoolSize(3)
    pool.setAcquireIncrement(1)
    pool.setMaxPoolSize(10)
    def connection = Session.create(pool.getConnection, a)
    SessionFactory.concreteFactory = Some(() => connection)
  }
  def clear {
    transaction {
      Mythos.drop
      Mythos.create
    }
  }
  def close {}

  def describe { Mythos.printDdl }
}

object Db extends Db
