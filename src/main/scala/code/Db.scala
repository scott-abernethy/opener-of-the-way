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
    // Connection pooling with c3p0
    val pool = new ComboPooledDataSource
    pool.setDriverClass(driver)
    pool.setJdbcUrl(url)
    pool.setUser(user)
    pool.setPassword(password)
    pool.setMinPoolSize(3)
    pool.setAcquireIncrement(1)
    pool.setMaxPoolSize(10)

    // Work around MySQL connection timeouts.
    pool.setMaxIdleTimeExcessConnections(30 * 60) // 30 mins
    pool.setMaxConnectionAge(4 * 60 * 60) // 4 hours
    pool.setIdleConnectionTestPeriod(1 * 60 * 60) // 1 hour

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
          val c1 = new Cultist
          c1.email = "foo@bar.com"
          c1.password = "foo"
          c1.insane = true
          val c2 = new Cultist
          c2.email = "two@bar.com"
          c2.password = "two"
          c2.expired = true

          val foo = Mythos.cultists.insert(c1)
          val two = Mythos.cultists.insert(c2)

          var g1: Gateway = new Gateway
          g1.cultistId = foo.id
          g1.location = "10.16.15.43/public"
          g1.path = "foobar"
          g1.localPath = ""
          g1.password = "treesaregreen"
          g1.source = true
          g1.sink = false
          g1 = Mythos.gateways.insert(g1)
          var g2: Gateway = new Gateway
          g2.cultistId = foo.id
          g2.location = "10.16.15.43/public"
          g2.path = "foobar-sink"
          g2.localPath = ""
          g2.password = "treesaregreen"
          g2.source = false
          g2.sink = true
          g2 = Mythos.gateways.insert(g2)
          var g3: Gateway = new Gateway
          g3.cultistId = two.id
          g3.location = "10.16.15.43/public"
          g3.path = "frog/sheep/cow"
          g3.localPath = ""
          g3.password = "cowsaregreen"
          g3.source = true
          g3.sink = false
          g3 = Mythos.gateways.insert(g3)

          val a = new Artifact
          a.gatewayId = g3.id
          a.path = "la/lo/lah"
          a.length = 8976L
          val b = new Artifact
          b.gatewayId = g3.id
          b.path = "la/foyhyyyyyyyy"
          b.length = 98512376L
          val c = new Artifact
          c.gatewayId = g3.id
          c.path = "la/lo/ppppp55"
          c.length = 8;
          val d = new Artifact
          d.gatewayId = g3.id
          d.path = "la/lo/913913.try0"
          d.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)
          d.length = 48395434523543L
          val e = new Artifact
          e.gatewayId = g1.id
          e.path = "mee/neigh"
          e.length = 3453456L
          val f = new Artifact
          f.gatewayId = g1.id
          f.path = "mee/oink"
          f.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)
          f.length = 23954345235437L
          val g = new Artifact
          g.gatewayId = g3.id
          g.path = "/var/cache/mv/outgoing/A Really Super Dooper Long File-name Which Could Cause Issues On Screen.archive.foo.bar.baz.mp3"
          g.witnessed = T.now
          g.length = 843562723L
          val h = new Artifact
          h.gatewayId = g3.id
          h.path = "other/one/foo.giz"
          h.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          h.discovered = T.ago(12 * 24 * 60 * 60 * 1000L)
          h.length = 25235454L
          val i = new Artifact
          i.gatewayId = g1.id
          i.path = "mee/nurfnurf"
          i.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          i.discovered = T.ago(5 * 24 * 60 * 60 * 1000L)
          i.length = 252345L
          val j = new Artifact
          j.gatewayId = g3.id
          j.path = "/fake"
          j.discovered = T.ago(60 * 60 * 1000L)
          j.witnessed = T.now
          j.length = 34;
          val k = new Artifact
          k.gatewayId = g3.id
          k.path = "/big.file"
          k.discovered = T.yesterday
          k.witnessed = T.now
          k.length = 43582342343L

          Mythos.artifacts.insert(a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: Nil)

          val clone1 = new Clone
          clone1.artifactId = 1
          clone1.forCultistId = foo.id
          clone1.state = CloneState.awaiting
          clone1.requested = T.now
          clone1.attempted = T.now
          clone1.attempts = 5
          Mythos.clones.insert(clone1)

          val clone4 = new Clone
          clone4.artifactId = 10
          clone4.forCultistId = foo.id
          clone4.state = CloneState.awaiting
          clone4.requested = T.yesterday
          clone4.attempted = T.yesterday
          clone4.attempts = 0
          Mythos.clones.insert(clone4)

          val presence4 = new Presence
          presence4.artifactId = 10
          presence4.state = PresenceState.called
          presence4.requested = T.yesterday
          presence4.attempted = T.yesterday
          presence4.attempts = 0
          Mythos.presences.insert(presence4)

          val clone3 = new Clone
          clone3.artifactId = 2
          clone3.forCultistId = foo.id
          clone3.state = CloneState.awaiting
          clone3.requested = T.ago(61 * 60 * 1000L)
          clone3.attempted = T.now
          clone3.attempts = 1
          Mythos.clones.insert(clone3)

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
