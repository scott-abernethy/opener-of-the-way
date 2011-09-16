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
  lazy val pool = {
    // Setup connection pooling with c3p0
    val pool = new ComboPooledDataSource
    pool.setDriverClass(driver)
    pool.setJdbcUrl(url)
    pool.setUser(user)
    pool.setPassword(password)
    pool.setMinPoolSize(3)
    pool.setAcquireIncrement(1)
    pool.setMaxPoolSize(10)
    pool
  }

  def init {
    Class.forName(driver)
    val adapterInstance: DatabaseAdapter = Class.forName(adapter).newInstance.asInstanceOf[DatabaseAdapter]
    SessionFactory.concreteFactory = Some(() => Session.create(pool.getConnection, adapterInstance))
  }

  def clear {
    transaction {
      Mythos.drop
      Mythos.create
    }
  }

  def close {
    pool.close()
  }

  def describe { Mythos.printDdl }

  def populate {
    Props.mode match {
      case Props.RunModes.Development =>
        clear
        transaction {
          val foo = Mythos.cultists.insert(new Cultist("foo@bar.com", "foo"))
          val two = Mythos.cultists.insert(new Cultist("two@bar.com", "two"))

          var g1: Gateway = new Gateway
          g1.cultistId = foo.id
          g1.location = "10.16.15.43/public"
          g1.path = "foobar"
          g1.localPath = ""
          g1.password = "treesaregreen"
          g1 = Mythos.gateways.insert(g1)
          var g2: Gateway = new Gateway
          g2.cultistId = foo.id
          g2.location = "10.16.15.43/public"
          g2.path = "foobar-sink"
          g2.localPath = ""
          g2.password = "treesaregreen"
          g2.mode = GateMode.sink
          g2 = Mythos.gateways.insert(g2)
          var g3: Gateway = new Gateway
          g3.cultistId = two.id
          g3.location = "10.16.15.43/public"
          g3.path = "frog/sheep/cow"
          g3.localPath = ""
          g3.password = "cowsaregreen"
          g3 = Mythos.gateways.insert(g3)

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
          val g = new Artifact
          g.gatewayId = g3.id
          g.path = "/var/cache/mv/outgoing/A Really Super Dooper Long File-name Which Could Cause Issues On Screen.archive.foo.bar.baz.mp3"
          g.witnessed = T.now
          val h = new Artifact
          h.gatewayId = g3.id
          h.path = "other/one/foo.giz"
          h.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          h.discovered = T.ago(12 * 24 * 60 * 60 * 1000L)
          val i = new Artifact
          i.gatewayId = g1.id
          i.path = "mee/nurfnurf"
          i.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          i.discovered = T.ago(5 * 24 * 60 * 60 * 1000L)

          Mythos.artifacts.insert(a :: b :: c :: d :: e :: f :: g :: h :: i :: Nil)

          val clone1 = new Clone
          clone1.artifactId = 1
          clone1.forCultistId = foo.id
          clone1.state = CloneState.cloning
          clone1.requested = T.yesterday
          clone1.attempted = T.yesterday
          clone1.attempts = 5
          Mythos.clones.insert(clone1)

          val clone2 = new Clone
          clone2.artifactId = 3
          clone2.forCultistId = foo.id
          clone2.state = CloneState.cloned
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
