package code

import gate.T
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

  def populate {
    Props.mode match {
      case Props.RunModes.Development =>
        Db.clear
        transaction {
          val foo = Mythos.cultists.insert(new Cultist("foo@bar.com", "foo"))
          val two = Mythos.cultists.insert(new Cultist("two@bar.com", "two"))
          val g1 = Mythos.gateways.insert(new Gateway(foo.id, "10.16.15.43/public", "foobar", "", "treesaregreen", GateMode.source, GateState.lost, code.gate.T.yesterday))
          val g2 = Mythos.gateways.insert(new Gateway(foo.id, "10.16.15.43/public", "foobar-sink", "", "treesaregreen", GateMode.sink, GateState.lost, code.gate.T.yesterday))
          val g3 = Mythos.gateways.insert(new Gateway(two.id, "10.16.15.43/public", "frog/sheep/cow", "", "cowsaregreen", GateMode.source, GateState.lost, code.gate.T.yesterday))

          val a = new Artifact
          a.gatewayId = g3.id
          a.path = "la/lo/lah"
          val b = new Artifact
          b.gatewayId = g3.id
          b.path = "la/foyhyyyyyyyy"
          val c = new Artifact
          c.gatewayId = g3.id
          c.path = "la/lo/ppppp55"
          val d = new Artifact
          d.gatewayId = g3.id
          d.path = "la/lo/913913.try0"
          d.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)
          val e = new Artifact
          e.gatewayId = g1.id
          e.path = "mee/neigh"
          val f = new Artifact
          f.gatewayId = g1.id
          f.path = "mee/oink"
          f.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)

          Mythos.artifacts.insert(a :: b :: c :: d :: e :: f :: Nil)

          val clone1 = new Clone
          clone1.artifactId = 1
          clone1.forCultistId = foo.id
          clone1.state = CloneState.progressing
          clone1.requested = T.yesterday
          clone1.attempted = T.yesterday
          clone1.attempts = 5
          Mythos.clones.insert(clone1)

          val clone2 = new Clone
          clone2.artifactId = 3
          clone2.forCultistId = foo.id
          clone2.state = CloneState.done
          clone2.requested = T.ago(89734562)
          clone2.attempted = T.ago(456789)
          clone2.attempts = 2
          clone2.duration = 123456
          Mythos.clones.insert(clone2)
        }
      case _ =>
    }
  }
}

object Db extends Db
